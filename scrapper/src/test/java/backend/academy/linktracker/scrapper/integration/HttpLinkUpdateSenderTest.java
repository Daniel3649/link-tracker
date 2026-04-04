package backend.academy.linktracker.scrapper.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import backend.academy.linktracker.scrapper.sender.HttpLinkUpdateSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpLinkUpdateSenderTest {

    @Test
    void shouldPreserveBadRequestMessageAfterLoggingErrorResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bot.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpLinkUpdateSender httpLinkUpdateSender = new HttpLinkUpdateSender(builder.build(), new ObjectMapper());

        server.expect(requestTo("https://bot.test/updates"))
                .andExpect(method(POST))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "description": "Некорректные параметры запроса",
                                  "code": "400",
                                  "exceptionName": "IllegalArgumentException",
                                  "exceptionMessage": "Broken payload",
                                  "stacktrace": []
                                }
                                """));

        LinkUpdate update =
                new LinkUpdate(1L, URI.create("https://github.com/octocat/Hello-World"), "updated", List.of(1L));

        assertThatThrownBy(() -> httpLinkUpdateSender.send(update))
                .isInstanceOf(BotClientException.class)
                .hasMessageContaining("Bot rejected update: Broken payload");

        server.verify();
    }
}
