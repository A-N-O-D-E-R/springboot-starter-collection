package com.anode.jira;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JiraDocumentsTest {

    @Test
    void paragraphsAndLineBreaks() {
        assertThat(JiraDocuments.text("line 1\nline 2\n\n<b>not markup</b>\n")).isEqualTo(Map.of(
                "type", "doc", "version", 1, "content", List.of(
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "line 1"),
                                Map.of("type", "hardBreak"),
                                Map.of("type", "text", "text", "line 2"))),
                        Map.of("type", "paragraph", "content", List.of(
                                Map.of("type", "text", "text", "<b>not markup</b>"))))));
    }
}
