package com.anode.confluence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.anode.confluence.ConfluenceClient.Body;
import com.anode.confluence.ConfluenceClient.NewPage;
import com.anode.confluence.ConfluenceClient.PageUpdate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class ConfluenceClientTest {

    private static final String API = "https://acme.atlassian.net/wiki/api/v2";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://acme.atlassian.net");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ConfluenceClient confluence = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
            .build().createClient(ConfluenceClient.class);

    @Test
    void findsASpaceByKey() {
        server.expect(requestTo(API + "/spaces?keys=DOC"))
                .andRespond(withSuccess("{\"results\":[{\"id\":\"98\",\"key\":\"DOC\",\"unknownField\":1}],"
                        + "\"_links\":{\"next\":\"/wiki/api/v2/spaces?cursor=x\"}}", MediaType.APPLICATION_JSON));

        var spaces = confluence.getSpaces(List.of("DOC"), null, null);

        assertThat(spaces.results()).extracting("id").containsExactly("98");
        assertThat(spaces.links().next()).endsWith("cursor=x");
    }

    @Test
    void readsAPageBody() {
        server.expect(requestTo(API + "/pages/7?body-format=storage"))
                .andRespond(withSuccess("{\"id\":\"7\",\"version\":{\"number\":3},"
                        + "\"body\":{\"storage\":{\"representation\":\"storage\",\"value\":\"<p>x</p>\"}}}", MediaType.APPLICATION_JSON));

        var page = confluence.getPage("7", "storage");

        assertThat(page.version().number()).isEqualTo(3);
        assertThat(page.body().get("storage").value()).isEqualTo("<p>x</p>");
    }

    @Test
    void createsAndUpdatesAPageInStorageFormat() {
        server.expect(method(HttpMethod.POST)).andExpect(requestTo(API + "/pages?root-level=true"))
                .andExpect(content().json("{\"spaceId\":\"98\",\"title\":\"Runbook\","
                        + "\"body\":{\"representation\":\"storage\",\"value\":\"<p>Restart the pump</p>\"}}", true))
                .andRespond(withSuccess("{\"id\":\"7\",\"title\":\"Runbook\"}", MediaType.APPLICATION_JSON));
        server.expect(method(HttpMethod.PUT)).andExpect(requestTo(API + "/pages/7"))
                .andExpect(content().json("{\"id\":\"7\",\"status\":\"current\",\"title\":\"Runbook\","
                        + "\"body\":{\"representation\":\"storage\",\"value\":\"<p>Done</p>\"},\"version\":{\"number\":2}}"))
                .andRespond(withSuccess("{\"id\":\"7\",\"version\":{\"number\":2}}", MediaType.APPLICATION_JSON));

        var page = confluence.createPage(new NewPage("98", "Runbook", null, Body.storage("<p>Restart the pump</p>")), true);
        confluence.updatePage(page.id(), PageUpdate.of(page.id(), "Runbook", Body.storage("<p>Done</p>"), 2));

        server.verify();
    }
}
