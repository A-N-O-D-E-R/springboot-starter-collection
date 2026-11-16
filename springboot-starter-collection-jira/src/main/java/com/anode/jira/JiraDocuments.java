package com.anode.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Builds Atlassian Document Format bodies (Jira Cloud API v3 comments) from plain text: blank lines
 * separate paragraphs, other line breaks are kept, nothing is interpreted as markup.
 */
public final class JiraDocuments {

    private static final Map<String, Object> HARD_BREAK = Map.of("type", "hardBreak");

    private JiraDocuments() {}

    /** An ADF document holding {@code text}. */
    public static Map<String, Object> text(String text) {
        List<Map<String, Object>> paragraphs = Arrays.stream(text.strip().split("\\R\\s*\\R"))
                .filter(block -> !block.isBlank())
                .map(JiraDocuments::paragraph)
                .toList();
        return Map.of("type", "doc", "version", 1, "content", paragraphs);
    }

    private static Map<String, Object> paragraph(String block) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (String line : block.strip().split("\\R")) {
            if (!content.isEmpty()) {
                content.add(HARD_BREAK);
            }
            content.add(Map.of("type", "text", "text", line));
        }
        return Map.of("type", "paragraph", "content", content);
    }
}
