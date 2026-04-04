package backend.academy.linktracker;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BotStartCommandE2ETest extends AbstractBotScrapperE2ETest {

    @Test
    void startCommand_shouldRegisterChatInScrapper() throws Exception {
        long chatId = 987654321L;

        resetMocks();
        stubTelegramGetUpdatesOnceThenEmpty("/start", chatId);
        stubTelegramSendMessageOk(chatId);
        stubTelegramSetMyCommandsOk();

        await().atMost(ASSERTION_TIMEOUT)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() ->
                        MOCK.verify(moreThanOrExactly(1), postRequestedFor(urlPathMatching("/bot[^/]+/getUpdates"))));

        await().atMost(ASSERTION_TIMEOUT).pollInterval(Duration.ofMillis(300)).untilAsserted(() -> {
            HttpResponse<String> response = getLinks(chatId);

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

            JsonNode json = OBJECT_MAPPER.readTree(response.body());
            assertThat(json.get("size").asInt()).isZero();
        });

        MOCK.verify(1, postRequestedFor(urlPathMatching("/bot[^/]+/sendMessage")));
    }
}
