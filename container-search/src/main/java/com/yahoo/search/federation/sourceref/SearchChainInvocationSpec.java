// Copyright Vespa.ai. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.federation.sourceref;

import com.yahoo.component.ComponentId;
import com.yahoo.search.Query;
import com.yahoo.search.Result;
import com.yahoo.search.searchchain.model.federation.FederationOptions;

import java.util.List;
import java.util.Objects;

/**
 * Specifies which search chain should be run and how it should be run.
 * This is a value object.
 *
 * @author Tony Vaagenes
 */
public class SearchChainInvocationSpec implements ModifyQueryAndResult, Cloneable {

    public final ComponentId searchChainId;

    /** The source to invoke, or null if none */
    public final ComponentId source;
    /** The provider to invoke, or null if none */
    public final ComponentId provider;

    public final FederationOptions federationOptions;
    public final List<String> schemas;

    public SearchChainInvocationSpec(ComponentId searchChainId, FederationOptions federationOptions, List<String> schemas) {
        this(searchChainId, null, null, federationOptions, schemas);
    }


    SearchChainInvocationSpec(ComponentId searchChainId, ComponentId source, ComponentId provider,
                              FederationOptions federationOptions, List<String> schemas) {
        this.searchChainId = searchChainId;
        this.source = source;
        this.provider = provider;
        this.federationOptions = federationOptions;
        this.schemas = List.copyOf(schemas);
    }

    @Override
    public SearchChainInvocationSpec clone() throws CloneNotSupportedException {
        return (SearchChainInvocationSpec)super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if ( ! (o instanceof SearchChainInvocationSpec other)) return false;

        if ( ! Objects.equals(this.searchChainId, other.searchChainId)) return false;
        if ( ! Objects.equals(this.source, other.source)) return false;
        if ( ! Objects.equals(this.provider, other.provider)) return false;
        if ( ! Objects.equals(this.federationOptions, other.federationOptions)) return false;
        if ( ! Objects.equals(this.schemas, other.schemas)) return false;
        return true;
    }

    @Override
    public int hashCode() { 
        return Objects.hash(searchChainId, source, provider, federationOptions, schemas);
    }

    public void modifyTargetQuery(Query query) { }
    public void modifyTargetResult(Result result) {}

}
