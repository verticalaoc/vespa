// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.nodeadmin;

import com.yahoo.concurrent.classlock.ClassLocking;
import com.yahoo.net.HostName;
import com.yahoo.system.ProcessExecuter;
import com.yahoo.vespa.hosted.dockerapi.Docker;
import com.yahoo.vespa.hosted.dockerapi.metrics.MetricReceiverWrapper;
import com.yahoo.vespa.hosted.node.admin.NodeAdminBaseConfig;
import com.yahoo.vespa.hosted.node.admin.component.AdminComponent;
import com.yahoo.vespa.hosted.node.admin.docker.DockerOperations;
import com.yahoo.vespa.hosted.node.admin.docker.DockerOperationsImpl;
import com.yahoo.vespa.hosted.node.admin.maintenance.StorageMaintainer;
import com.yahoo.vespa.hosted.node.admin.maintenance.acl.AclMaintainer;
import com.yahoo.vespa.hosted.node.admin.nodeagent.NodeAgent;
import com.yahoo.vespa.hosted.node.admin.nodeagent.NodeAgentImpl;
import com.yahoo.vespa.hosted.node.admin.noderepository.NodeRepository;
import com.yahoo.vespa.hosted.node.admin.noderepository.NodeRepositoryImpl;
import com.yahoo.vespa.hosted.node.admin.orchestrator.Orchestrator;
import com.yahoo.vespa.hosted.node.admin.orchestrator.OrchestratorImpl;
import com.yahoo.vespa.hosted.node.admin.util.ConfigServerHttpRequestExecutor;
import com.yahoo.vespa.hosted.node.admin.util.Environment;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/**
 * Component that manages Docker containers based on some node repository.
 */
public class DockerAdminComponent implements AdminComponent {
    private static final Duration NODE_AGENT_SCAN_INTERVAL = Duration.ofSeconds(30);
    private static final Duration NODE_ADMIN_CONVERGE_STATE_INTERVAL = Duration.ofSeconds(30);

    private final NodeAdminBaseConfig nodeAdminBaseConfig;
    private final NodeAdminConfig config;
    private final Docker docker;
    private final MetricReceiverWrapper metricReceiver;
    private final ClassLocking classLocking;

    private ConfigServerHttpRequestExecutor requestExecutor;

    private Optional<NodeAdminStateUpdater> nodeAdminStateUpdater = Optional.empty();

    public DockerAdminComponent(NodeAdminBaseConfig nodeAdminBaseConfig,
                                NodeAdminConfig config,
                                Docker docker,
                                MetricReceiverWrapper metricReceiver,
                                ClassLocking classLocking) {
        this.nodeAdminBaseConfig = nodeAdminBaseConfig;
        this.config = config;
        this.docker = docker;
        this.metricReceiver = metricReceiver;
        this.classLocking = classLocking;
    }

    @Override
    public void enable() {
        if (nodeAdminStateUpdater.isPresent()) {
            return;
        }

        Environment environment = new Environment();
        requestExecutor = ConfigServerHttpRequestExecutor.create(nodeAdminBaseConfig.configServerConfig());
        NodeRepository nodeRepository = new NodeRepositoryImpl(requestExecutor);
        Orchestrator orchestrator = new OrchestratorImpl(requestExecutor);

        Clock clock = Clock.systemUTC();
        String dockerHostHostName = HostName.getLocalhost();
        ProcessExecuter processExecuter = new ProcessExecuter();

        docker.start();
        DockerOperations dockerOperations = new DockerOperationsImpl(
                docker,
                nodeAdminBaseConfig.configServerConfig(),
                environment,
                processExecuter);

        StorageMaintainer storageMaintainer = new StorageMaintainer(
                nodeAdminBaseConfig,
                dockerOperations,
                processExecuter,
                metricReceiver,
                environment,
                clock);

        AclMaintainer aclMaintainer = new AclMaintainer(
                dockerOperations,
                nodeRepository,
                dockerHostHostName);

        Function<String, NodeAgent> nodeAgentFactory = (hostName) -> new NodeAgentImpl(
                dockerHostHostName,
                hostName,
                nodeRepository,
                orchestrator,
                dockerOperations,
                storageMaintainer,
                aclMaintainer,
                clock,
                NODE_AGENT_SCAN_INTERVAL);

        NodeAdmin nodeAdmin = new NodeAdminImpl(
                dockerOperations,
                nodeAgentFactory,
                storageMaintainer,
                aclMaintainer,
                metricReceiver,
                clock);

        nodeAdminStateUpdater = Optional.of(new NodeAdminStateUpdater(
                nodeRepository,
                orchestrator,
                storageMaintainer,
                nodeAdmin,
                dockerHostHostName,
                clock,
                NODE_ADMIN_CONVERGE_STATE_INTERVAL,
                classLocking));

        nodeAdminStateUpdater.get().start();
    }

    @Override
    public void disable() {
        if (!nodeAdminStateUpdater.isPresent()) {
            return;
        }

        nodeAdminStateUpdater.get().stop();
        requestExecutor.close();
        nodeAdminStateUpdater = Optional.empty();
        // TODO: Also stop docker
    }

    public NodeAdminStateUpdater getNodeAdminStateUpdater() {
        return nodeAdminStateUpdater.get();
    }
}
