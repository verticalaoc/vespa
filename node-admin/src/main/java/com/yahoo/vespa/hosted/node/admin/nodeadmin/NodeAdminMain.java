// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.nodeadmin;

import com.yahoo.component.ComponentId;
import com.yahoo.component.provider.ComponentRegistry;
import com.yahoo.concurrent.classlock.ClassLocking;
import com.yahoo.log.LogLevel;
import com.yahoo.vespa.defaults.Defaults;
import com.yahoo.vespa.hosted.dockerapi.Docker;
import com.yahoo.vespa.hosted.dockerapi.metrics.MetricReceiverWrapper;
import com.yahoo.vespa.hosted.node.admin.NodeAdminBaseConfig;
import com.yahoo.vespa.hosted.node.admin.component.AdminComponent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * NodeAdminMain is the main component of the node admin JDisc application:
 *  - It will read config and check its environment to figure out its responsibilities
 *  - It will "start" (only) the necessary components.
 *  - Other components MUST NOT try to start (typically in constructor) since the features
 *    they provide is NOT WANTED and possibly destructive, and/or the environment may be
 *    incompatible. For instance, trying to contact the Docker daemon too early will
 *    be fatal: the node admin may not have installed and started the docker daemon.
 */
public class NodeAdminMain implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(NodeAdminMain.class.getName());

    private final ComponentRegistry<AdminComponent> adminRegistry;
    private final NodeAdminBaseConfig nodeAdminBaseConfig;
    private final Docker docker;
    private final MetricReceiverWrapper metricReceiver;
    private final ClassLocking classLocking;

    private List<AdminComponent> enabledComponents = new ArrayList<>();

    private Optional<DockerAdminComponent> dockerAdmin = Optional.empty();

    public NodeAdminMain(ComponentRegistry<AdminComponent> adminRegistry,
                         NodeAdminBaseConfig nodeAdminBaseConfig,
                         Docker docker,
                         MetricReceiverWrapper metricReceiver,
                         ClassLocking classLocking) {
        this.adminRegistry = adminRegistry;
        this.nodeAdminBaseConfig = nodeAdminBaseConfig;
        this.docker = docker;
        this.metricReceiver = metricReceiver;
        this.classLocking = classLocking;
    }

    public static NodeAdminConfig getConfig() {
        String path = Defaults.getDefaults().underVespaHome("conf/node-admin.json");
        return NodeAdminConfig.fromFile(new File(path));
    }

    public void start() {
        // TODO: Combine the configs
        NodeAdminConfig config = getConfig();

        if (config.components.isEmpty()) {
            dockerAdmin = Optional.of(new DockerAdminComponent(
                    nodeAdminBaseConfig, config, docker, metricReceiver, classLocking));
            enable(dockerAdmin.get());
        } else {
            logger.log(LogLevel.INFO, () -> {
                String registeredComponentsList = adminRegistry
                        .allComponentsById().keySet().stream()
                        .map(ComponentId::stringValue)
                        .collect(Collectors.joining(", "));

                String requestedComponentsList = config.components.stream()
                        .collect(Collectors.joining(", "));

                return String.format(
                        "Components registered = '%s', enabled = '%s'",
                        registeredComponentsList,
                        requestedComponentsList);
            });

            for (String componentSpecificationString : config.components) {
                AdminComponent component =
                        adminRegistry.getComponent(componentSpecificationString);
                if (component == null) {
                    throw new IllegalArgumentException("There is no component named '" +
                            componentSpecificationString + "'");
                }
                enable(component);
            }
        }
    }

    private void enable(AdminComponent component) {
        component.enable();
        enabledComponents.add(component);
    }

    @Override
    public void close() {
        int i = enabledComponents.size();
        while (i --> 0) {
            enabledComponents.remove(i).disable();
        }
    }

    public NodeAdminStateUpdater getNodeAdminStateUpdater() {
        return dockerAdmin.get().getNodeAdminStateUpdater();
    }
}
