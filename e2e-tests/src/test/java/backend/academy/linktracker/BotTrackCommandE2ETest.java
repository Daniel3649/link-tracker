package backend.academy.linktracker;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BotTrackCommandE2ETest extends AbstractBotScrapperE2ETest {

    @Test
    void trackCommand_shouldAddGithubLinkToScrapper() throws Exception {
        long chatId = 987654322L;
        URI link = URI.create("https://github.com/octocat/Hello-World");

        resetMocks();
        stubTelegramTrackFlow(chatId, link, "work, hobby");
        stubTelegramSendMessageOk(chatId);
        stubTelegramSetMyCommandsOk();
        stubGitHubEndpoints();

        await().atMost(ASSERTION_TIMEOUT)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() ->
                        MOCK.verify(moreThanOrExactly(1), postRequestedFor(urlPathMatching("/bot[^/]+/getUpdates"))));

        await().ignoreExceptions()
                .atMost(ASSERTION_TIMEOUT)
                .pollInterval(Duration.ofMillis(300))
                .untilAsserted(() -> {
                    HttpResponse<String> response = getLinks(chatId);

                    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());

                    JsonNode json = OBJECT_MAPPER.readTree(response.body());
                    assertThat(json.get("size").asInt()).isEqualTo(1);

                    JsonNode first = json.get("links").get(0);
                    assertThat(first.get("url").asText()).isEqualTo(link.toString());
                    assertThat(first.get("tags").toString()).contains("work");
                    assertThat(first.get("tags").toString()).contains("hobby");
                });
    }
}
