package io.github.biojava.spring.examples;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureIO;
import org.biojava.nbio.structure.StructureTools;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StructureExampleTest {

    @Autowired
    private AtomCache atomCache;

    @Test
    void loadStructure() throws Exception {
        Structure structure = StructureIO.getStructure("4HHB");
        assertThat(structure).isNotNull();
        assertThat(StructureTools.getNrAtoms(structure)).isGreaterThan(0);
        assertThat(structure.getPDBCode()).isEqualTo("4HHB");
    }

    @Test
    void loadBiologicalAssembly() throws Exception {
        Structure structure = StructureIO.getBiologicalAssembly("1GAV", 1);
        assertThat(structure).isNotNull();
        assertThat(structure.getChains()).isNotEmpty();
    }

    @Test
    void cacheIntegration() throws Exception {
        assertThat(atomCache).isNotNull();
        assertThat(atomCache.getPath()).isNotEmpty();
    }
}
