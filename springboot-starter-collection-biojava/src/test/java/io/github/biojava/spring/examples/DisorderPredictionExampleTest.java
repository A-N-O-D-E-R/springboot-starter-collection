package io.github.biojava.spring.examples;

import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.ronn.Jronn;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DisorderPredictionExampleTest {

    @Test
    void predictDisorder() throws Exception {
        ProteinSequence seq = new ProteinSequence(
            "MSTNPKPQRKTKRNTNRRPQDVKFPGGGQIVGGVYLLPRRGPRLGVRATRKTSERSQPRGRRQPIPKARRPEGRTQE" +
            "REQKAIGVKPCPIPNPLLGLDSTRTGHHHHHH"
        );

        float[] scores = Jronn.getDisorderScores(seq);

        assertThat(scores).isNotNull();
        assertThat(scores.length).isEqualTo(seq.getLength());
        assertThat(scores).allMatch(score -> score >= 0 && score <= 1);
    }
}
