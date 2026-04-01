package backend.academy.linktracker.bot.integration.conversation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.BotApplication;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@ActiveProfiles("test")
@SpringBootTest(classes = BotApplication.class, properties = "app.telegram.url=http://localhost:9999/bot")
@EnableWireMock({@ConfigureWireMock(name = "scrapper", baseUrlProperties = "app.scrapper.base-url")})
class TrackDialogTest {

    @InjectWireMock("scrapper")
    private WireMockServer wireMock;

    @Autowired
    private TelegramUpdateService updateService;

    @MockitoBean
    private TelegramSender telegramSender;

    @MockitoBean
    private TelegramBot telegramBot;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        Mockito.clearInvocations(telegramSender, telegramBot);
    }

    @Test
    void shouldSendAddLinkRequestAfterTrackDialog() {
        long chatId = 123456L;
        URI link = URI.create("https://github.com/octocat/Hello-World");

        wireMock.stubFor(post(urlEqualTo("/links")).willReturn(okJson(linkResponseJson(1L, link, "work", "hobby"))));

        updateService.handleEvent(update(1, chatId, "/track"));
        updateService.handleEvent(update(2, chatId, link.toString()));
        updateService.handleEvent(update(3, chatId, "work, hobby"));

        wireMock.verify(1, postRequestedForLinks(chatId, link.toString()));
    }

    @Test
    void shouldNotifyUserWhenTrackLinkIsInvalid() {
        long chatId = 123457L;
        String invalidLink = "not a uri";

        updateService.handleEvent(update(1, chatId, "/track"));
        Mockito.clearInvocations(telegramSender);

        updateService.handleEvent(update(2, chatId, invalidLink));

        List<String> messages = capturedMessages(chatId);

        assertThat(messages).anySatisfy(text -> {
            String normalized = text.toLowerCase();
            assertThat(normalized.contains("incorrect") || normalized.contains("uri")).isTrue();
        });

        wireMock.verify(0, postRequestedFor(urlEqualTo("/links")));
    }

    @Test
    void shouldNotifyUserWhenLinkIsAlreadyTrackedDuringTrackDialog() {
        long chatId = 223344L;
        URI link = URI.create("https://github.com/octocat/Hello-World");

        wireMock.stubFor(post(urlEqualTo("/links"))
                .inScenario("duplicate-track")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("already-tracked")
                .willReturn(okJson(linkResponseJson(1L, link, "work", "hobby"))));

        wireMock.stubFor(post(urlEqualTo("/links"))
                .inScenario("duplicate-track")
                .whenScenarioStateIs("already-tracked")
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(apiErrorJson("Link is already tracked", "Link is already tracked", "409"))));

        updateService.handleEvent(update(1, chatId, "/track"));
        updateService.handleEvent(update(2, chatId, link.toString()));
        updateService.handleEvent(update(3, chatId, "work, hobby"));

        Mockito.clearInvocations(telegramSender);

        updateService.handleEvent(update(4, chatId, "/track"));
        updateService.handleEvent(update(5, chatId, link.toString()));
        updateService.handleEvent(update(6, chatId, "work, hobby"));

        List<String> messages = capturedMessages(chatId);

        assertThat(messages).anySatisfy(text -> {
            String normalized = text.toLowerCase();
            assertThat(normalized.contains("already") || normalized.contains("follow"))
                    .isTrue();
        });

        wireMock.verify(2, postRequestedForLinks(chatId, link.toString()));
    }

    @Test
    void shouldSendActiveSubscriptionsListWhenUserRequestsList() {
        long chatId = 345678L;

        URI firstLink = URI.create("https://github.com/octocat/Hello-World");
        URI secondLink = URI.create("https://github.com/spring-projects/spring-boot");

        wireMock.stubFor(get(urlEqualTo("/links"))
                .withHeader("Tg-Chat-Id", equalTo(String.valueOf(chatId)))
                .willReturn(okJson(listLinksResponseJson(
                        linkResponseJson(1L, firstLink, "work"), linkResponseJson(2L, secondLink, "study")))));

        updateService.handleEvent(update(1, chatId, "/list"));

        List<String> messages = capturedMessages(chatId);

        assertThat(messages).anySatisfy(text -> {
            String normalized = text.toLowerCase();
            assertThat(normalized).contains(firstLink.toString().toLowerCase());
            assertThat(normalized).contains(secondLink.toString().toLowerCase());
        });

        wireMock.verify(
                1, getRequestedFor(urlEqualTo("/links")).withHeader("Tg-Chat-Id", equalTo(String.valueOf(chatId))));
    }

    @Test
    void shouldSendNoActiveSubscriptionsMessageWhenUserRequestsListAndHasNoSubscriptions() {
        long chatId = 445566L;

        wireMock.stubFor(get(urlEqualTo("/links"))
                .withHeader("Tg-Chat-Id", equalTo(String.valueOf(chatId)))
                .willReturn(okJson("""
                {
                  "links": [],
                  "size": 0
                }
                """)));

        updateService.handleEvent(update(1, chatId, "/list"));

        List<String> messages = capturedMessages(chatId);

        assertThat(messages).anySatisfy(text -> assertThat(text.toLowerCase()).contains("empty"));
    }

    @Test
    void shouldSendOnlySubscriptionsWithRequestedTagWhenUserRequestsListByTag() {
        long chatId = 556677L;

        URI workLink1 = URI.create("https://github.com/octocat/Hello-World");
        URI workLink2 = URI.create("https://github.com/spring-projects/spring-boot");
        URI studyLink = URI.create("https://github.com/openjdk/jdk");

        wireMock.stubFor(get(urlEqualTo("/links"))
                .withHeader("Tg-Chat-Id", equalTo(String.valueOf(chatId)))
                .willReturn(okJson(listLinksResponseJson(
                        linkResponseJson(1L, workLink1, "work"),
                        linkResponseJson(2L, workLink2, "backend", "work"),
                        linkResponseJson(3L, studyLink, "study")))));

        updateService.handleEvent(update(1, chatId, "/list work"));

        List<String> messages = capturedMessages(chatId);

        assertThat(messages).anySatisfy(text -> {
            String normalized = text.toLowerCase();
            assertThat(normalized).contains(workLink1.toString().toLowerCase());
            assertThat(normalized).contains(workLink2.toString().toLowerCase());
            assertThat(normalized).doesNotContain(studyLink.toString().toLowerCase());
        });
    }

    private List<String> capturedMessages(long chatId) {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(telegramSender, atLeastOnce()).sendPlain(eq(chatId), messageCaptor.capture());

        return messageCaptor.getAllValues();
    }

    private Update update(int updateId, long chatId, String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);

        when(update.updateId()).thenReturn(updateId);
        when(update.message()).thenReturn(message);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn(text);
        when(chat.id()).thenReturn(chatId);

        return update;
    }

    private static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder postRequestedForLinks(
            long chatId, String link) {
        return postRequestedFor(urlEqualTo("/links"))
                .withHeader("Tg-Chat-Id", equalTo(String.valueOf(chatId)))
                .withRequestBody(matchingJsonPath("$.link", equalTo(link)));
    }

    private static String linkResponseJson(long id, URI url, String... tags) {
        String tagsJson = Arrays.stream(tags).map(tag -> "\"" + tag + "\"").collect(Collectors.joining(", "));

        return """
            {
              "id": %d,
              "url": "%s",
              "tags": [%s],
              "filters": []
            }
            """.formatted(id, url, tagsJson);
    }

    private static String listLinksResponseJson(String... links) {
        String linksJson = String.join(",", links);

        return """
            {
              "links": [%s],
              "size": %d
            }
            """.formatted(linksJson, links.length);
    }

    private static String apiErrorJson(String description, String exceptionMessage, String code) {
        return """
            {
              "description": "%s",
              "code": "%s",
              "exceptionName": "TestException",
              "exceptionMessage": "%s",
              "stacktrace": []
            }
            """.formatted(description, code, exceptionMessage);
    }
}
