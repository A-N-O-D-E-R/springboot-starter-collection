package io.github.biojava.spring.examples;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.RNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.biojava.nbio.core.sequence.io.FastaReaderHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SequenceExampleTest {

    @Test
    void createDnaSequence() throws Exception {
        DNASequence dna = new DNASequence("ATCG");
        assertThat(dna.getSequenceAsString()).isEqualTo("ATCG");
        assertThat(dna.getLength()).isEqualTo(4);
    }

    @Test
    void createRnaSequence() throws Exception {
        RNASequence rna = new RNASequence("AUCG");
        assertThat(rna.getSequenceAsString()).isEqualTo("AUCG");
    }

    @Test
    void createProteinSequence() throws Exception {
        ProteinSequence protein = new ProteinSequence("MSTNPKPQRKTKRNTNRRPQDVKFPGG");
        assertThat(protein.getLength()).isEqualTo(27);
    }

    @Test
    void handleAmbiguityDnaSequence() throws Exception {
        AmbiguityDNACompoundSet ambiguitySet = AmbiguityDNACompoundSet.getDNACompoundSet();
        DNASequence ambiguous = new DNASequence("ATCGWWW", ambiguitySet);
        assertThat(ambiguous.getLength()).isEqualTo(7);
    }

    @Test
    void readFastaFile() throws Exception {
        Path tempFasta = Files.createTempFile("test", ".fasta");
        Files.writeString(tempFasta, ">seq1\nATCGATCG\n>seq2\nGCTAGCTA\n");

        LinkedHashMap<String, DNASequence> sequences =
            FastaReaderHelper.readFastaDNASequence(tempFasta.toFile());

        assertThat(sequences).hasSize(2);
        assertThat(sequences.get("seq1").getSequenceAsString()).isEqualTo("ATCGATCG");
        assertThat(sequences.get("seq2").getSequenceAsString()).isEqualTo("GCTAGCTA");

        Files.deleteIfExists(tempFasta);
    }
}
