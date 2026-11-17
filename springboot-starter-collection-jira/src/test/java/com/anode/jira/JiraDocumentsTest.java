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

    @Test
    void markdownHeadingsListsCheckboxesAndMarks() {
        var doc = JiraDocuments.markdown("# Title\n\nSome **bold** and `code`\nsame paragraph\n\n- [x] done\n- [ ] todo\n- plain");
        @SuppressWarnings("unchecked")
        var content = (List<Map<String, Object>>) doc.get("content");
        assertThat(content).extracting(n -> n.get("type")).containsExactly("heading", "paragraph", "bulletList");
        assertThat(content.get(0).get("attrs")).isEqualTo(Map.of("level", 1));
        assertThat(content.get(1).get("content")).isEqualTo(List.of(
                Map.of("type", "text", "text", "Some "),
                Map.of("type", "text", "text", "bold", "marks", List.of(Map.of("type", "strong"))),
                Map.of("type", "text", "text", " and "),
                Map.of("type", "text", "text", "code", "marks", List.of(Map.of("type", "code"))),
                Map.of("type", "text", "text", " same paragraph")));
        assertThat(content.get(2).toString()).contains("☑ done", "☐ todo", "plain");
    }

    @Test
    void markdownOfNothingIsAnEmptyDocument() {
        assertThat(JiraDocuments.markdown(null)).isEqualTo(Map.of("type", "doc", "version", 1, "content", List.of()));
    }
}
