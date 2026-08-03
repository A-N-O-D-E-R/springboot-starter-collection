package io.github.biojava.spring.examples;

import io.github.biojava.spring.BioJavaProperties;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BioJavaAutoConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void autoConfigurationLoads() {
        assertThat(context.getBean(AtomCache.class)).isNotNull();
        assertThat(context.getBean(BioJavaProperties.class)).isNotNull();
    }

    @Test
    void cacheConfiguredFromProperties() {
        AtomCache cache = context.getBean(AtomCache.class);
        assertThat(cache.getPath()).contains("biojava-test-cache");
    }
}
