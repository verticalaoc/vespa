// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.logging;

import com.yahoo.yolean.trace.TraceNode;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static java.util.Objects.requireNonNull;

/**
 * A immutable request log entry
 *
 * @author bjorncs
 */
public class RequestLogEntry {

    private final String connectionId;
    private final Instant timestamp;
    private final Duration duration;
    private final int localPort;
    private final String peerAddress;
    private final int peerPort;
    private final String remoteAddress;
    private final int remotePort;
    private final String userAgent;
    private final String referer;
    private final String httpMethod;
    private final String httpVersion;
    private final String hostString;
    private final int statusCode;
    private final long contentSize;
    private final String scheme;
    private final String rawPath;
    private final String rawQuery;
    private final Principal userPrincipal;
    private final HitCounts hitCounts;
    private final TraceNode traceNode;
    private final Map<String, Collection<String>> extraAttributes;

    private RequestLogEntry(Builder builder) {
        this.connectionId = builder.connectionId;
        this.timestamp = builder.timestamp;
        this.duration = builder.duration;
        this.localPort = builder.localPort;
        this.peerAddress = builder.peerAddress;
        this.peerPort = builder.peerPort;
        this.remoteAddress = builder.remoteAddress;
        this.remotePort = builder.remotePort;
        this.userAgent = builder.userAgent;
        this.referer = builder.referer;
        this.httpMethod = builder.httpMethod;
        this.httpVersion = builder.httpVersion;
        this.hostString = builder.hostString;
        this.statusCode = builder.statusCode;
        this.contentSize = builder.contentSize;
        this.scheme = builder.scheme;
        this.rawPath = builder.rawPath;
        this.rawQuery = builder.rawQuery;
        this.userPrincipal = builder.userPrincipal;
        this.hitCounts = builder.hitCounts;
        this.traceNode = builder.traceNode;
        this.extraAttributes = copyExtraAttributes(builder.extraAttributes);
    }

    public Optional<String> connectionId() { return Optional.ofNullable(connectionId); }
    public Optional<Instant> timestamp() { return Optional.ofNullable(timestamp); }
    public Optional<Duration> duration() { return Optional.ofNullable(duration); }
    public OptionalInt localPort() { return optionalInt(localPort); }
    public Optional<String> peerAddress() { return Optional.ofNullable(peerAddress); }
    public OptionalInt peerPort() { return optionalInt(peerPort); }
    public Optional<String> remoteAddress() { return Optional.ofNullable(remoteAddress); }
    public OptionalInt remotePort() { return optionalInt(remotePort); }
    public Optional<String> userAgent() { return Optional.ofNullable(userAgent); }
    public Optional<String> referer() { return Optional.ofNullable(referer); }
    public Optional<String> httpMethod() { return Optional.ofNullable(httpMethod); }
    public Optional<String> httpVersion() { return Optional.ofNullable(httpVersion); }
    public Optional<String> hostString() { return Optional.ofNullable(hostString); }
    public OptionalInt statusCode() { return optionalInt(statusCode); }
    public OptionalLong contentSize() { return optionalLong(contentSize); }
    public Optional<String> scheme() { return Optional.ofNullable(scheme); }
    public Optional<String> rawPath() { return Optional.ofNullable(rawPath); }
    public Optional<String> rawQuery() { return Optional.ofNullable(rawQuery); }
    public Optional<Principal> userPrincipal() { return Optional.ofNullable(userPrincipal); }
    public Optional<HitCounts> hitCounts() { return Optional.ofNullable(hitCounts); }
    public Optional<TraceNode> traceNode() { return Optional.ofNullable(traceNode); }
    public Collection<String> extraAttributeKeys() { return Collections.unmodifiableCollection(extraAttributes.keySet()); }
    public Collection<String> extraAttributeValues(String key) { return Collections.unmodifiableCollection(extraAttributes.get(key)); }

    private static OptionalInt optionalInt(int value) {
        if (value == -1) return OptionalInt.empty();
        return OptionalInt.of(value);
    }

    private static OptionalLong optionalLong(long value) {
        if (value == -1) return OptionalLong.empty();
        return OptionalLong.of(value);
    }

    private static Map<String, Collection<String>> copyExtraAttributes(Map<String, Collection<String>> extraAttributes) {
        Map<String, Collection<String>> copy = new HashMap<>();
        extraAttributes.forEach((key, value) -> copy.put(key, new ArrayList<>(value)));
        return copy;
    }

    public static class Builder {

        private String connectionId;
        private Instant timestamp;
        private Duration duration;
        private int localPort = -1;
        private String peerAddress;
        private int peerPort = -1;
        private String remoteAddress;
        private int remotePort = -1;
        private String userAgent;
        private String referer;
        private String httpMethod;
        private String httpVersion;
        private String hostString;
        private int statusCode = -1;
        private long contentSize = -1;
        private String scheme;
        private String rawPath;
        private String rawQuery;
        private Principal userPrincipal;
        private HitCounts hitCounts;
        private TraceNode traceNode;
        private final Map<String, Collection<String>> extraAttributes = new HashMap<>();

        public Builder connectionId(String connectionId) { this.connectionId = requireNonNull(connectionId); return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = requireNonNull(timestamp); return this; }
        public Builder duration(Duration duration) { this.duration = requireNonNull(duration); return this; }
        public Builder localPort(int localPort) { this.localPort = requireNonNegative(localPort); return this; }
        public Builder peerAddress(String peerAddress) { this.peerAddress = requireNonNull(peerAddress); return this; }
        public Builder peerPort(int peerPort) { this.peerPort = requireNonNegative(peerPort); return this; }
        public Builder remoteAddress(String remoteAddress) { this.remoteAddress = requireNonNull(remoteAddress); return this; }
        public Builder remotePort(int remotePort) { this.remotePort = requireNonNegative(remotePort); return this; }
        public Builder userAgent(String userAgent) { this.userAgent = requireNonNull(userAgent); return this; }
        public Builder referer(String referer) { this.referer = requireNonNull(referer); return this; }
        public Builder httpMethod(String httpMethod) { this.httpMethod = requireNonNull(httpMethod); return this; }
        public Builder httpVersion(String httpVersion) { this.httpVersion = requireNonNull(httpVersion); return this; }
        public Builder hostString(String hostString) { this.hostString = requireNonNull(hostString); return this; }
        public Builder statusCode(int statusCode) { this.statusCode = requireNonNegative(statusCode); return this; }
        public Builder contentSize(long contentSize) { this.contentSize = requireNonNegative(contentSize); return this; }
        public Builder scheme(String scheme) { this.scheme = requireNonNull(scheme); return this; }
        public Builder rawPath(String rawPath) { this.rawPath = requireNonNull(rawPath); return this; }
        public Builder rawQuery(String rawQuery) { this.rawQuery = requireNonNull(rawQuery); return this; }
        public Builder userPrincipal(Principal userPrincipal) { this.userPrincipal = requireNonNull(userPrincipal); return this; }
        public Builder hitCounts(HitCounts hitCounts) { this.hitCounts = requireNonNull(hitCounts); return this; }
        public Builder traceNode(TraceNode traceNode) { this.traceNode = requireNonNull(traceNode); return this; }
        public Builder addExtraAttribute(String key, String value) {
            this.extraAttributes.computeIfAbsent(requireNonNull(key), __ -> new ArrayList<>()).add(requireNonNull(value));
            return this;
        }
        public Builder addExtraAttributes(String key, Collection<String> values) {
            this.extraAttributes.computeIfAbsent(requireNonNull(key), __ -> new ArrayList<>()).addAll(requireNonNull(values));
            return this;
        }
        public RequestLogEntry build() { return new RequestLogEntry(this); }

        private static int requireNonNegative(int value) {
            if (value < 0) throw new IllegalArgumentException("Value must be non-negative: " + value);
            return value;
        }

        private static long requireNonNegative(long value) {
            if (value < 0) throw new IllegalArgumentException("Value must be non-negative: " + value);
            return value;
        }
    }
}
