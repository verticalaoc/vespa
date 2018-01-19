// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.util;

import com.google.common.base.Strings;
import com.yahoo.vespa.hosted.dockerapi.ContainerName;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Various utilities for getting values from node-admin's environment. Immutable.
 *
 * @author bakksjo
 * @author hmusum
 */
public class Environment {
    private static final DateFormat filenameFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public static final String APPLICATION_STORAGE_CLEANUP_PATH_PREFIX = "cleanup_";

    private static final String LOGSTASH_NODES = "LOGSTASH_NODES";

    private final InetAddressResolver inetAddressResolver;
    private final PathResolver pathResolver;
    private final List<String> logstashNodes;

    static {
        filenameFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public Environment() {
        this(new InetAddressResolver(),
             new PathResolver(),
             getLogstashNodesFromEnvironment());
    }

    public Environment(InetAddressResolver inetAddressResolver,
                       PathResolver pathResolver,
                       List<String> logstashNodes) {
        this.inetAddressResolver = inetAddressResolver;
        this.pathResolver = pathResolver;
        this.logstashNodes = logstashNodes;
    }

    private static List<String> getLogstashNodesFromEnvironment() {
        String logstashNodes = System.getenv(LOGSTASH_NODES);
        if(Strings.isNullOrEmpty(logstashNodes)) {
            return Collections.emptyList();
        }
        return Arrays.asList(logstashNodes.split("[,\\s]+"));
    }

    public InetAddress getInetAddressForHost(String hostname) throws UnknownHostException {
        return inetAddressResolver.getInetAddressForHost(hostname);
    }

    public PathResolver getPathResolver() {
        return pathResolver;
    }

    /**
     * Absolute path in node admin to directory with processed and reported core dumps
     */
    public Path pathInNodeAdminToDoneCoredumps() {
        return pathResolver.getApplicationStoragePathForNodeAdmin().resolve("processed-coredumps");
    }

    /**
     * Absolute path in node admin container to the node cleanup directory.
     */
    public Path pathInNodeAdminToNodeCleanup(ContainerName containerName) {
        return pathResolver.getApplicationStoragePathForNodeAdmin()
                .resolve(APPLICATION_STORAGE_CLEANUP_PATH_PREFIX + containerName.asString() +
                        "_" + filenameFormatter.format(Date.from(Instant.now())));
    }

    /**
     * Translates an absolute path in node agent container to an absolute path in node admin container.
     * @param containerName name of the node agent container
     * @param absolutePathInNode absolute path in that container
     * @return the absolute path in node admin container pointing at the same inode
     */
    public Path pathInNodeAdminFromPathInNode(ContainerName containerName, String absolutePathInNode) {
        Path pathInNode = Paths.get(absolutePathInNode);
        if (! pathInNode.isAbsolute()) {
            throw new IllegalArgumentException("The specified path in node was not absolute: " + absolutePathInNode);
        }

        return pathResolver.getApplicationStoragePathForNodeAdmin()
                .resolve(containerName.asString())
                .resolve(PathResolver.ROOT.relativize(pathInNode));
    }

    /**
     * Translates an absolute path in node agent container to an absolute path in host.
     * @param containerName name of the node agent container
     * @param absolutePathInNode absolute path in that container
     * @return the absolute path in host pointing at the same inode
     */
    public Path pathInHostFromPathInNode(ContainerName containerName, String absolutePathInNode) {
        Path pathInNode = Paths.get(absolutePathInNode);
        if (! pathInNode.isAbsolute()) {
            throw new IllegalArgumentException("The specified path in node was not absolute: " + absolutePathInNode);
        }

        return pathResolver.getApplicationStoragePathForHost()
                .resolve(containerName.asString())
                .resolve(PathResolver.ROOT.relativize(pathInNode));
    }

    public List<String> getLogstashNodes() {
        return logstashNodes;
    }


    public static class Builder {
        private InetAddressResolver inetAddressResolver;
        private PathResolver pathResolver;
        private List<String> logstashNodes = Collections.emptyList();


        public Builder inetAddressResolver(InetAddressResolver inetAddressResolver) {
            this.inetAddressResolver = inetAddressResolver;
            return this;
        }

        public Builder pathResolver(PathResolver pathResolver) {
            this.pathResolver = pathResolver;
            return this;
        }

        public Builder logstashNodes(List<String> hosts) {
            this.logstashNodes = hosts;
            return this;
        }


        public Environment build() {
            return new Environment(inetAddressResolver, pathResolver, logstashNodes);
        }
    }
}
