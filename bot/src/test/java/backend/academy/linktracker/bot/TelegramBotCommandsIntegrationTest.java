package backend.academy.linktracker.bot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.bot.properties.TelegramProperties;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.service.UpdateService;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
public class TelegramBotCommandsIntegrationTest {

    @Autowired
    TelegramBot telegramBot;

    @Autowired
    TelegramProperties telegramProperties;

    @Autowired
    UpdateService updateService;

    @Autowired
    MessageService messageService;

    @AfterEach
    void clearUpdatesListener() {
        telegramBot.removeGetUpdatesListener();
    }

    @Test
    void startCommand_sendsGreeting() throws InterruptedException {
        long chatId = 987654321L;

        stubGetUpdatesOnceThenEmpty("/start", chatId);
        stubSendMessageOk(chatId);

        CountDownLatch latch = new CountDownLatch(1);
        telegramBot.setUpdatesListener(updates -> {
            updates.forEach(updateService::handleEvent);
            latch.countDown();
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        assertTrue(latch.await(10, SECONDS));

        String expectedText = messageService.get("command.start");

        await().atMost(10, SECONDS)
                .untilAsserted(() -> verify(
                        1,
                        postRequestedFor(urlPathTemplate("/bot{token}/sendMessage"))
                                .withPathParam("token", equalTo(telegramProperties.getToken()))
                                .withRequestBody(matchingBodyContainsChatId(chatId))
                                .withRequestBody(matchingBodyContainsText(expectedText))));
    }

    @Test
    void helpCommand_sendsHelp() throws InterruptedException {
        long chatId = 987654321L;

        stubGetUpdatesOnceThenEmpty("/help", chatId);
        stubSendMessageOk(chatId);

        CountDownLatch latch = new CountDownLatch(1);
        telegramBot.setUpdatesListener(updates -> {
            updates.forEach(updateService::handleEvent);
            latch.countDown();
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        assertTrue(latch.await(10, SECONDS));

        String expectedText = messageService.get("command.help.header") + '\n' + "/help - "
                + messageService.get("command.help.description") + '\n' + "/start - "
                + messageService.get("command.start.description") + '\n';

        await().atMost(10, SECONDS)
                .untilAsserted(() -> verify(
                        1,
                        postRequestedFor(urlPathTemplate("/bot{token}/sendMessage"))
                                .withPathParam("token", equalTo(telegramProperties.getToken()))
                                .withRequestBody(matchingBodyContainsChatId(chatId))
                                .withRequestBody(matchingBodyContainsText(expectedText))));
    }

    @Test
    void unknownCommand_sendsError() throws InterruptedException {
        long chatId = 987654321L;

        stubGetUpdatesOnceThenEmpty("/abracadabra", chatId);
        stubSendMessageOk(chatId);

        CountDownLatch latch = new CountDownLatch(1);
        telegramBot.setUpdatesListener(updates -> {
            updates.forEach(updateService::handleEvent);
            latch.countDown();
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

        assertTrue(latch.await(10, SECONDS));

        String expectedText = messageService.get("command.unknown");

        await().atMost(10, SECONDS)
                .untilAsserted(() -> verify(
                        1,
                        postRequestedFor(urlPathTemplate("/bot{token}/sendMessage"))
                                .withPathParam("token", equalTo(telegramProperties.getToken()))
                                .withRequestBody(matchingBodyContainsChatId(chatId))
                                .withRequestBody(matchingBodyContainsText(expectedText))));
    }

    private void stubGetUpdatesOnceThenEmpty(String text, long chatId) {
        stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("cmd")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                      "ok": true,
                      "result": [
                        {
                          "update_id": 1,
                          "message": {
                            "message_id": 1,
                            "from": { "id": 1, "is_bot": false, "first_name": "Test" },
                            "chat": { "id": %d, "type": "private" },
                            "date": 1700000000,
                            "text": "%s"
                          }
                        }
                      ]
                    }
                    """.formatted(chatId, escapeJson(text))))
                .willSetStateTo("EMPTY"));

        stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario("cmd")
                .whenScenarioStateIs("EMPTY")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    { "ok": true, "result": [] }
                    """)));
    }

    private void stubSendMessageOk(long chatId) {
        stubFor(post(urlMatching("/bot[^/]+/sendMessage"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                    {
                      "ok": true,
                      "result": {
                        "message_id": 1,
                        "date": 1700000000,
                        "chat": { "id": %d, "type": "private" },
                        "text": "ok"
                      }
                    }
                    """.formatted(chatId))));
    }

    private static ContentPattern<?> matchingBodyContainsChatId(long chatId) {
        return matching("(?s).*(\"chat_id\"\\s*:\\s*" + chatId + "|chat_id=" + chatId + ").*");
    }

    private static ContentPattern<?> matchingBodyContainsText(String text) {
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");

        String json = "\"text\"\\s*:\\s*\"" + Pattern.quote(text) + "\"";
        String form = "text=" + Pattern.quote(encoded);

        return matching("(?s).*(" + json + "|" + form + ").*");
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
