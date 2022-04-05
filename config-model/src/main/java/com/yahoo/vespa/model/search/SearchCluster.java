// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.model.search;

import com.yahoo.config.model.deploy.DeployState;
import com.yahoo.config.model.producer.UserConfigRepo;
import com.yahoo.searchdefinition.RankProfile;
import com.yahoo.searchdefinition.RankProfileRegistry;
import com.yahoo.searchdefinition.Schema;
import com.yahoo.searchdefinition.derived.RawRankProfile;
import com.yahoo.searchdefinition.derived.SummaryMap;
import com.yahoo.searchlib.rankingexpression.Reference;
import com.yahoo.tensor.TensorType;
import com.yahoo.vespa.config.search.RankProfilesConfig;
import com.yahoo.vespa.config.search.SummaryConfig;
import com.yahoo.prelude.fastsearch.DocumentdbInfoConfig;
import com.yahoo.vespa.config.search.SummarymapConfig;
import com.yahoo.search.config.IndexInfoConfig;
import com.yahoo.vespa.config.search.AttributesConfig;
import com.yahoo.vespa.configdefinition.IlscriptsConfig;
import com.yahoo.searchdefinition.derived.DerivedConfiguration;
import com.yahoo.config.model.producer.AbstractConfigProducer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a search cluster.
 *
 * @author arnej27959
 */
public abstract class SearchCluster extends AbstractConfigProducer<SearchCluster>
        implements
        DocumentdbInfoConfig.Producer,
        IndexInfoConfig.Producer,
        IlscriptsConfig.Producer {

    private final String clusterName;
    private int index;
    private Double queryTimeout;
    private Double visibilityDelay = 0.0;
    private final List<String> documentNames = new ArrayList<>();
    private final List<SchemaInfo> schemas = new ArrayList<>();

    public SearchCluster(AbstractConfigProducer<?> parent, String clusterName, int index) {
        super(parent, "cluster." + clusterName);
        this.clusterName = clusterName;
        this.index = index;
    }

    /** Returns the writable list of schemas that should be active in this cluster. */
    public List<SchemaInfo> schemas() { return schemas; }

    /**
     * Must be called after cluster is built, to derive schema configs
     * Derives the schemas from the application package.
     * Also stores the document names contained in the schemas.
     */
    public void deriveSchemas(DeployState deployState) {
    }

    /**
     * Converts summary and summary map config to the appropriate information in documentdb
     *
     * @param summaryConfigProducer the summary config
     * @param summarymapConfigProducer the summary map config, or null if none is available
     * @param docDb the target document dm config
     */
    protected void convertSummaryConfig(SummaryConfig.Producer summaryConfigProducer,
                                        SummarymapConfig.Producer summarymapConfigProducer,
                                        DocumentdbInfoConfig.Documentdb.Builder docDb) {

        SummaryConfig.Builder summaryConfigBuilder = new SummaryConfig.Builder();
        summaryConfigProducer.getConfig(summaryConfigBuilder);
        SummaryConfig summaryConfig = summaryConfigBuilder.build();

        SummarymapConfig summarymapConfig = null;
        if (summarymapConfigProducer != null) {
            SummarymapConfig.Builder summarymapConfigBuilder = new SummarymapConfig.Builder();
            summarymapConfigProducer.getConfig(summarymapConfigBuilder);
            summarymapConfig = summarymapConfigBuilder.build();
        }

        for (SummaryConfig.Classes sclass : summaryConfig.classes()) {
            DocumentdbInfoConfig.Documentdb.Summaryclass.Builder sumClassBuilder = new DocumentdbInfoConfig.Documentdb.Summaryclass.Builder();
            sumClassBuilder.
                id(sclass.id()).
                name(sclass.name());
            for (SummaryConfig.Classes.Fields field : sclass.fields()) {
                DocumentdbInfoConfig.Documentdb.Summaryclass.Fields.Builder fieldsBuilder = new DocumentdbInfoConfig.Documentdb.Summaryclass.Fields.Builder();
                fieldsBuilder.name(field.name())
                             .type(field.type())
                             .dynamic(isDynamic(field.name(), summarymapConfig));
                sumClassBuilder.fields(fieldsBuilder);
            }
            docDb.summaryclass(sumClassBuilder);
        }
    }

    /** Returns true if this is a dynamic summary field */
    private boolean isDynamic(String fieldName, SummarymapConfig summarymapConfig) {
        if (summarymapConfig == null) return false; // not know for streaming, but also not used

        for (SummarymapConfig.Override override : summarymapConfig.override()) {
            if ( ! fieldName.equals(override.field())) continue;
            if (SummaryMap.isDynamicCommand(override.command())) return true;
        }
        return false;
    }

    protected void addRankProfilesConfig(DocumentdbInfoConfig.Documentdb.Builder docDbBuilder, RankProfilesConfig rankProfilesCfg) {
        for (RankProfilesConfig.Rankprofile rankProfile : rankProfilesCfg.rankprofile()) {
            DocumentdbInfoConfig.Documentdb.Rankprofile.Builder rpB = new DocumentdbInfoConfig.Documentdb.Rankprofile.Builder();
            rpB.name(rankProfile.name());
            rpB.hasSummaryFeatures(containsPropertiesWithPrefix(RawRankProfile.summaryFeatureFefPropertyPrefix, rankProfile.fef()));
            rpB.hasRankFeatures(containsPropertiesWithPrefix(RawRankProfile.rankFeatureFefPropertyPrefix, rankProfile.fef()));
            docDbBuilder.rankprofile(rpB);
        }
    }

    private boolean containsPropertiesWithPrefix(String prefix, RankProfilesConfig.Rankprofile.Fef fef) {
        for (RankProfilesConfig.Rankprofile.Fef.Property p : fef.property()) {
            if (p.name().startsWith(prefix))
                return true;
        }
        return false;
    }

    public void addDocumentNames(Schema schema) {
        documentNames.add(schema.getDocument().getDocumentName().getName());
    }

    /** Returns a List with document names used in this search cluster */
    public List<String> getDocumentNames() { return documentNames; }

    public String getClusterName()              { return clusterName; }
    public final String getIndexingModeName()   { return getIndexingMode().getName(); }
    public final boolean isStreaming()          { return getIndexingMode() == IndexingMode.STREAMING; }

    public final SearchCluster setQueryTimeout(Double to) {
        this.queryTimeout = to;
        return this;
    }

    public final SearchCluster setVisibilityDelay(double delay) {
        this.visibilityDelay = delay;
        return this;
    }

    protected abstract IndexingMode getIndexingMode();
    public final Double getVisibilityDelay() { return visibilityDelay; }
    public final Double getQueryTimeout() { return queryTimeout; }
    public abstract int getRowBits();
    public final void setClusterIndex(int index) { this.index = index; }
    public final int getClusterIndex() { return index; }

    public abstract void defaultDocumentsConfig();
    public abstract DerivedConfiguration getSchemaConfig();

    @Override
    public void getConfig(IndexInfoConfig.Builder builder) {
        if (getSchemaConfig() != null) getSchemaConfig().getIndexInfo().getConfig(builder);
    }

    @Override
    public void getConfig(IlscriptsConfig.Builder builder) {
        if (getSchemaConfig() != null) getSchemaConfig().getIndexingScript().getConfig(builder);
    }

    // TODO: Remove?
    public void getConfig(AttributesConfig.Builder builder) {
        if (getSchemaConfig() != null) getSchemaConfig().getConfig(builder);
    }

    // TODO: Remove?
    public void getConfig(RankProfilesConfig.Builder builder) {
        if (getSchemaConfig() != null) getSchemaConfig().getRankProfileList().getConfig(builder);
    }

    @Override
    public abstract void getConfig(DocumentdbInfoConfig.Builder builder);

    @Override
    public String toString() { return "search-capable cluster '" + clusterName + "'"; }

    public static final class IndexingMode {

        public static final IndexingMode REALTIME  = new IndexingMode("REALTIME");
        public static final IndexingMode STREAMING = new IndexingMode("STREAMING");

        private final String name;

        private IndexingMode(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        public String toString() {
            return "indexingmode: " + name;
        }
    }

    public static final class SchemaInfo {

        private final Schema schema;
        private final UserConfigRepo userConfigRepo;

        // Info about profiles needed in memory after build.
        // The rank profile registry itself is not kept around due to its size.
        private final Map<String, RankProfileInfo> rankProfiles;

        public SchemaInfo(Schema schema, UserConfigRepo userConfigRepo, RankProfileRegistry rankProfileRegistry) {
            this.schema = schema;
            this.userConfigRepo = userConfigRepo;

            rankProfiles = rankProfileRegistry.rankProfilesOf(schema)
                                              .stream()
                                              .map(p -> new RankProfileInfo(p))
                                              .collect(Collectors.toUnmodifiableMap(p -> p.name(), p -> p) );
        }

        public Schema fullSchema() { return schema; }
        public UserConfigRepo userConfigs() { return userConfigRepo; }

    }

    /** A store of a *small* (in memory) amount of rank profile info. */
    public static final class RankProfileInfo {

        private final String name;
        private final Map<Reference, TensorType> inputs;

        public RankProfileInfo(RankProfile profile) {
            this.name = profile.name();
            this.inputs = profile.inputs();
        }

        public String name() { return name; }

        public Map<Reference, TensorType> inputs() { return inputs; }

    }

}
