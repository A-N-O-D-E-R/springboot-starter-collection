package io.github.biojava.spring.examples;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.align.StructureAlignment;
import org.biojava.nbio.structure.align.StructureAlignmentFactory;
import org.biojava.nbio.structure.align.ce.CeCPMain;
import org.biojava.nbio.structure.align.ce.CeMain;
import org.biojava.nbio.structure.align.model.AFPChain;
import org.biojava.nbio.structure.align.multiple.MultipleAlignment;
import org.biojava.nbio.structure.align.multiple.mc.MultipleMcMain;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AlignmentExampleTest {

    @Autowired
    private AtomCache atomCache;

    @Test
    void pairwiseStructureAlignment() throws Exception {
        Atom[] ca1 = atomCache.getAtoms("3cna.A");
        Atom[] ca2 = atomCache.getAtoms("2pel");

        StructureAlignment algorithm =
            StructureAlignmentFactory.getAlgorithm(CeCPMain.algorithmName);
        AFPChain afpChain = algorithm.align(ca1, ca2);

        assertThat(afpChain).isNotNull();
        assertThat(afpChain.getTMScore()).isGreaterThan(0);
        assertThat(afpChain.getAlnLength()).isGreaterThan(0);
    }

    @Test
    void multipleStructureAlignment() throws Exception {
        List<String> names = List.of("3app", "4ape", "5pep");
        List<Atom[]> atoms = names.stream()
            .map(name -> {
                try {
                    return atomCache.getAtoms(name);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();

        StructureAlignment pairwise =
            StructureAlignmentFactory.getAlgorithm(CeMain.algorithmName);
        MultipleMcMain multiple = new MultipleMcMain(pairwise);
        MultipleAlignment result = multiple.align(atoms);

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }
}
