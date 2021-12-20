// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.di;

import com.yahoo.config.ConfigInstance;
import com.yahoo.config.test.Bootstrap1Config;
import com.yahoo.config.test.Bootstrap2Config;
import com.yahoo.config.test.TestConfig;
import com.yahoo.container.di.ConfigRetriever.BootstrapConfigs;
import com.yahoo.container.di.ConfigRetriever.ComponentsConfigs;
import com.yahoo.container.di.ConfigRetriever.ConfigSnapshot;
import com.yahoo.vespa.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author gjoranv
 * @author Tony Vaagenes
 * @author ollivir
 */
public class ConfigRetrieverTest {

    private DirConfigSource dirConfigSource = null;

    @Before
    public void setup() {
        dirConfigSource = new DirConfigSource("ConfigRetrieverTest-");
    }

    @After
    public void cleanup() {
        dirConfigSource.cleanup();
    }

    @Test
    public void require_that_bootstrap_configs_come_first() {
        writeConfigs();
        ConfigRetriever retriever = createConfigRetriever();
        ConfigSnapshot bootstrapConfigs = retriever.getConfigs(Collections.emptySet(), 0, true);

        assertTrue(bootstrapConfigs instanceof BootstrapConfigs);
    }

    @Test
    @SuppressWarnings("unused")
    public void require_that_components_comes_after_bootstrap() {
        writeConfigs();
        ConfigRetriever retriever = createConfigRetriever();
        ConfigSnapshot bootstrapConfigs = retriever.getConfigs(Collections.emptySet(), 0, true);

        ConfigKey<? extends ConfigInstance> testConfigKey = new ConfigKey<>(TestConfig.class, dirConfigSource.configId());
        ConfigSnapshot componentsConfigs = retriever.getConfigs(Collections.singleton(testConfigKey), 0, true);

        if (componentsConfigs instanceof ComponentsConfigs) {
            assertEquals(3, componentsConfigs.size());
        } else {
            fail("ComponentsConfigs has unexpected type: " + componentsConfigs);
        }
    }

    @Ignore
    @SuppressWarnings("unused")
    public void require_exception_upon_modified_components_keys_without_bootstrap() {

        writeConfigs();
        ConfigRetriever retriever = createConfigRetriever();
        ConfigKey<? extends ConfigInstance> testConfigKey = new ConfigKey<>(TestConfig.class, dirConfigSource.configId());
        ConfigSnapshot bootstrapConfigs = retriever.getConfigs(Collections.emptySet(), 0, true);
        ConfigSnapshot componentsConfigs = retriever.getConfigs(Collections.singleton(testConfigKey), 0, true);
        Set<ConfigKey<? extends ConfigInstance>> keys = new HashSet<>();
        keys.add(testConfigKey);
        keys.add(new ConfigKey<>(TestConfig.class, ""));
        try {
            retriever.getConfigs(keys, 0, true);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("", e.getMessage());
        }
    }

    @Test
    public void require_that_empty_components_keys_after_bootstrap_returns_components_configs() {
        writeConfigs();
        ConfigRetriever retriever = createConfigRetriever();
        assertTrue(retriever.getConfigs(Collections.emptySet(), 0, true) instanceof BootstrapConfigs);
        assertTrue(retriever.getConfigs(Collections.emptySet(), 0, true) instanceof ComponentsConfigs);
    }

    public void writeConfigs() {
        writeConfig("bootstrap1", "dummy \"ignored\"");
        writeConfig("bootstrap2", "dummy \"ignored\"");
        writeConfig("test", "stringVal \"ignored\"");
    }

    private ConfigRetriever createConfigRetriever() {
        String configId = dirConfigSource.configId();
        CloudSubscriberFactory subscriberFactory = new CloudSubscriberFactory(dirConfigSource.configSource());
        Set<ConfigKey<? extends ConfigInstance>> keys = new HashSet<>();
        keys.add(new ConfigKey<>(Bootstrap1Config.class, configId));
        keys.add(new ConfigKey<>(Bootstrap2Config.class, configId));
        return new ConfigRetriever(keys, subscriberFactory);
    }

    private void writeConfig(String name, String contents) {
        dirConfigSource.writeConfig(name, contents);
    }
}
