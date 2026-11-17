package com.anode.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds Atlassian Document Format bodies (Jira Cloud API v3 comments and descriptions): {@link #text} from plain
 * text (blank lines separate paragraphs, other line breaks are kept, nothing is interpreted as markup),
 * {@link #markdown} from a small markdown subset.
 */
public final class JiraDocuments {

    private static final Map<String, Object> HARD_BREAK = Map.of("type", "hardBreak");

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*]\\s+(\\[[ xX]\\]\\s+)?(.*)$");
    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`");

    private JiraDocuments() {}

    /**
     * An ADF document from markdown: headings, paragraphs, bullet lists, task-style checkboxes (rendered as
     * ☑/☐), {@code **bold**} and {@code `code`}. Covers what the apps produce; anything richer needs a real parser.
     */
    public static Map<String, Object> markdown(String markdown) {
        List<Object> content = new ArrayList<>();
        List<Object> bullets = null;
        StringBuilder paragraph = new StringBuilder();
        for (String line : (markdown == null ? "" : markdown).split("\\R")) {
            Matcher heading = HEADING.matcher(line);
            Matcher bullet = BULLET.matcher(line);
            if (heading.matches() || bullet.matches() || line.isBlank()) {
                flush(content, paragraph);
            }
            if (heading.matches()) {
                bullets = null;
                content.add(Map.of("type", "heading", "attrs", Map.of("level", heading.group(1).length()),
                        "content", inline(heading.group(2))));
            } else if (bullet.matches()) {
                if (bullets == null) {
                    bullets = new ArrayList<>();
                    content.add(Map.of("type", "bulletList", "content", bullets));
                }
                String box = bullet.group(1) == null ? "" : bullet.group(1).toLowerCase().contains("x") ? "☑ " : "☐ ";
                bullets.add(Map.of("type", "listItem", "content",
                        List.of(Map.of("type", "paragraph", "content", inline(box + bullet.group(2))))));
            } else if (line.isBlank()) {
                bullets = null;
            } else {
                bullets = null;
                paragraph.append(paragraph.isEmpty() ? "" : " ").append(line.strip());
            }
        }
        flush(content, paragraph);
        return Map.of("type", "doc", "version", 1, "content", content);
    }

    private static void flush(List<Object> content, StringBuilder paragraph) {
        if (!paragraph.isEmpty()) {
            content.add(Map.of("type", "paragraph", "content", inline(paragraph.toString())));
            paragraph.setLength(0);
        }
    }

    /** Text with **bold** and `code`; ADF rejects empty text nodes. */
    private static List<Object> inline(String text) {
        List<Object> nodes = new ArrayList<>();
        Matcher m = INLINE.matcher(text);
        int i = 0;
        while (m.find()) {
            if (m.start() > i) {
                nodes.add(Map.of("type", "text", "text", text.substring(i, m.start())));
            }
            String mark = m.group(1) != null ? "strong" : "code";
            nodes.add(Map.of("type", "text", "text", m.group(1) != null ? m.group(1) : m.group(2),
                    "marks", List.of(Map.of("type", mark))));
            i = m.end();
        }
        if (i < text.length()) {
            nodes.add(Map.of("type", "text", "text", text.substring(i)));
        }
        return nodes.isEmpty() ? List.of(Map.of("type", "text", "text", " ")) : nodes;
    }

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
