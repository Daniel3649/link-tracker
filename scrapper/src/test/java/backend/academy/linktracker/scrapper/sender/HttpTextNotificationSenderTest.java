package backend.academy.linktracker.scrapper.sender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;

import backend.academy.linktracker.contract.dto.request.TextNotification;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpTextNotificationSenderTest {

    @Test
    void shouldPreserveBadRequestMessageAfterLoggingErrorResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bot.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpTextNotificationSender sender = new HttpTextNotificationSender(builder.build(), new ObjectMapper());

        server.expect(requestTo("https://bot.test/notifications/text"))
                .andExpect(method(POST))
                .andRespond(
                        withBadRequest().contentType(MediaType.APPLICATION_JSON).body("""
                                {
                                  "description": "Некорректные параметры запроса",
                                  "code": "400",
                                  "exceptionName": "IllegalArgumentException",
                                  "exceptionMessage": "Broken notification payload",
                                  "stacktrace": []
                                }
                                """));

        TextNotification notification = new TextNotification("Link check report", List.of(1L));

        assertThatThrownBy(() -> sender.send(notification))
                .isInstanceOf(BotClientException.class)
                .hasMessageContaining("Bot rejected text notification: Broken notification payload");

        server.verify();
    }
}
