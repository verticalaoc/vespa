// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.server.maintenance;

import com.yahoo.cloud.config.ConfigserverConfig;
import com.yahoo.config.FileReference;
import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.subscription.ConfigSourceSet;
import com.yahoo.jrt.Supervisor;
import com.yahoo.jrt.Transport;
import com.yahoo.vespa.config.JRTConnectionPool;
import com.yahoo.vespa.config.server.ApplicationRepository;
import com.yahoo.vespa.config.server.session.Session;
import com.yahoo.vespa.config.server.session.SessionRepository;
import com.yahoo.vespa.config.server.tenant.Tenant;
import com.yahoo.vespa.curator.Curator;
import com.yahoo.vespa.defaults.Defaults;
import com.yahoo.vespa.filedistribution.FileDownloader;
import com.yahoo.vespa.filedistribution.FileReferenceDownload;
import com.yahoo.vespa.flags.FlagSource;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.yahoo.vespa.config.server.filedistribution.FileDistributionUtil.fileReferenceExistsOnDisk;
import static com.yahoo.vespa.config.server.filedistribution.FileDistributionUtil.getOtherConfigServersInCluster;

/**
 * Verifies that all active sessions has an application package on local disk.
 * If not, the package is downloaded with file distribution. This can happen e.g.
 * if a configserver is down when the application is deployed.
 *
 * @author gjoranv
 */
public class ApplicationPackageMaintainer extends ConfigServerMaintainer {

    private static final Logger log = Logger.getLogger(ApplicationPackageMaintainer.class.getName());

    private final ApplicationRepository applicationRepository;
    private final File downloadDirectory;
    private final ConfigserverConfig configserverConfig;
    private final Supervisor supervisor;

    ApplicationPackageMaintainer(ApplicationRepository applicationRepository,
                                 Curator curator,
                                 Duration interval,
                                 FlagSource flagSource) {
        super(applicationRepository, curator, flagSource, applicationRepository.clock().instant(), interval, false);
        this.applicationRepository = applicationRepository;
        this.configserverConfig = applicationRepository.configserverConfig();
        this.supervisor = new Supervisor(new Transport("filedistribution-pool")).setDropEmptyBuffers(true);
        downloadDirectory = new File(Defaults.getDefaults().underVespaHome(configserverConfig.fileReferencesDir()));
    }

    @Override
    protected double maintain() {
        if (getOtherConfigServersInCluster(configserverConfig).isEmpty()) return 1.0; // Nothing to do

        final AtomicInteger attempts = new AtomicInteger(0);
        final AtomicInteger failures = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try (var fileDownloader = createFileDownloader()) {
            for (var applicationId : applicationRepository.listApplications()) {
                log.fine(() -> "Verifying application package for " + applicationId);
                Session session = applicationRepository.getActiveSession(applicationId);
                if (session == null) continue;  // App might be deleted after call to listApplications()

                FileReference applicationPackage = session.getApplicationPackageReference();
                if (applicationPackage == null) continue;

                if ( ! fileReferenceExistsOnDisk(downloadDirectory, applicationPackage)) {
                    long sessionId = session.getSessionId();
                    log.fine(() -> "Downloading application package for " + applicationId +
                            " application package reference " + applicationPackage +
                            " (session " + sessionId + ")");

                    FileReferenceDownload download = new FileReferenceDownload(applicationPackage,
                                                                               false,
                                                                               this.getClass().getSimpleName());
                    futures.add(CompletableFuture.supplyAsync(() -> fileDownloader.getFile(download))
                                                 .thenAccept(file -> {
                                                     if (file.isPresent()) {
                                                         attempts.incrementAndGet();
                                                         createLocalSessionIfMissing(applicationId, sessionId);
                                                     } else {
                                                         failures.incrementAndGet();
                                                         log.warning("Failed to download application package for application " +
                                                                             applicationId + " (session " + sessionId + ")");
                                                     }
                                                 }));
                }
            }
        }
        log.fine(() -> "Attempts: " + attempts.get() + ", failures: " + failures.get());
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.log(Level.WARNING, "Failed to get future", e);
            }
        });
        return asSuccessFactor(attempts.get(), failures.get());
    }

    private FileDownloader createFileDownloader() {
        return new FileDownloader(new JRTConnectionPool(new ConfigSourceSet(getOtherConfigServersInCluster(configserverConfig)), supervisor),
                                  supervisor,
                                  downloadDirectory);
    }

    @Override
    public void awaitShutdown() {
        supervisor.transport().shutdown().join();
        super.awaitShutdown();
    }

    private void createLocalSessionIfMissing(ApplicationId applicationId, long sessionId) {
        Tenant tenant = applicationRepository.getTenant(applicationId);
        SessionRepository sessionRepository = tenant.getSessionRepository();
        if (sessionRepository.getLocalSession(sessionId) == null)
            sessionRepository.createLocalSessionFromDistributedApplicationPackage(sessionId);
    }

}
