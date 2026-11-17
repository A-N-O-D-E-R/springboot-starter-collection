package com.anode.confluence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * Confluence Cloud REST API v2 ({@code <site>/wiki/api/v2}): spaces, pages, child pages, footer comments.
 * Optional parameters are skipped when {@code null}; {@code bodyFormat} is {@code "storage"} or
 * {@code "atlas_doc_format"}. Records carry the fields used here, unknown ones are ignored.
 */
@HttpExchange("/wiki/api/v2")
public interface ConfluenceClient {

    /** Spaces, e.g. by key: a space's id is what pages refer to. */
    @GetExchange("/spaces")
    Results<Space> getSpaces(
            @RequestParam(name = "keys", required = false) List<String> keys,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit);

    /** Pages, e.g. by space and title. */
    @GetExchange("/pages")
    Results<Page> getPages(
            @RequestParam(name = "space-id", required = false) List<String> spaceIds,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "body-format", required = false) String bodyFormat,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit);

    @GetExchange("/pages/{id}")
    Page getPage(@PathVariable("id") String id,
            @RequestParam(name = "body-format", required = false) String bodyFormat);

    /** {@code rootLevel} puts the page at the space's root rather than under its homepage. */
    @PostExchange("/pages")
    Page createPage(@RequestBody NewPage page,
            @RequestParam(name = "root-level", required = false) Boolean rootLevel);

    /** {@code update}'s version number must be the current one plus 1. */
    @PutExchange("/pages/{id}")
    Page updatePage(@PathVariable("id") String id, @RequestBody PageUpdate update);

    @GetExchange("/pages/{id}/children")
    Results<Page> getChildPages(@PathVariable("id") String id,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) Integer limit);

    @PostExchange("/footer-comments")
    Comment createFooterComment(@RequestBody NewComment comment);

    /** A page of results; {@code links.next} is the following one's URL, {@code null} on the last. */
    record Results<T>(List<T> results, @JsonProperty("_links") Links links) {}

    record Links(String next, String webui, String base) {}

    record Space(String id, String key, String name, String type, String status, String homepageId) {}

    /** {@code body} is keyed by representation ({@code storage}, {@code atlas_doc_format}) when asked for. */
    record Page(String id, String status, String title, String spaceId, String parentId, Version version,
            Map<String, Body> body, @JsonProperty("_links") Links links) {}

    record Version(Integer number, String message) {}

    /** {@code value} is storage-format XHTML, or an ADF document serialized as JSON. */
    record Body(String representation, String value) {
        public static Body storage(String xhtml) {
            return new Body("storage", xhtml);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record NewPage(String spaceId, String title, String parentId, Body body) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PageUpdate(String id, String status, String title, Body body, Version version) {
        /** The current page {@code id} retitled/rewritten as {@code version}. */
        public static PageUpdate of(String id, String title, Body body, int version) {
            return new PageUpdate(id, "current", title, body, new Version(version, null));
        }
    }

    record NewComment(String pageId, Body body) {}

    record Comment(String id, String pageId, Version version) {}
}
