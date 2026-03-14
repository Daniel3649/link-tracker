package backend.academy.linktracker.bot.integration.conversation;

import backend.academy.linktracker.bot.BotApplication;
import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.repository.TrackDialogStateRepository;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.TelegramUpdateService;
import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.clients.github.GitHubClient;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryGithubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryStackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemorySubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemorySubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryTelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryTrackedLinkRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(
    classes = BotApplication.class
)
class TrackDialogTest {

    private static ConfigurableApplicationContext scrapperContext;
    private static int scrapperPort;

    @Autowired
    private TelegramUpdateService updateService;

    @Autowired
    private ScrapperClient scrapperClient;

    @Autowired
    private TrackDialogStateRepository trackDialogStateRepository;

    @MockitoBean
    private TelegramSender telegramSender;

    @MockitoBean
    private TelegramBot telegramBot;

    @BeforeAll
    static void startScrapper() {
        if (scrapperContext == null) {
            scrapperContext = new SpringApplicationBuilder(
                ScrapperApplication.class,
                ScrapperTestConfig.class
            )
                .properties(
                    "spring.main.allow-bean-definition-overriding=true",
                    "server.port=0",
                    "spring.profiles.active=test",

                    "app.bot.base-url=http://localhost:65535",

                    "app.github.base-url=https://api.github.com",
                    "app.github.token=dummy-token",

                    "app.stackoverflow.base-url=https://api.stackexchange.com/2.3",
                    "app.stackoverflow.access-token=dummy-access-token",
                    "app.stackoverflow.key=dummy-key",

                    "app.scheduler.link-check-delay-ms=60000"
                )
                .run();

            ServletWebServerApplicationContext webContext =
                (ServletWebServerApplicationContext) scrapperContext;

            scrapperPort = webContext.getWebServer().getPort();
        }
    }

    @AfterAll
    static void stopScrapper() {
        if (scrapperContext != null) {
            scrapperContext.close();
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        if (scrapperContext == null) {
            startScrapper();
        }

        registry.add("app.scrapper.base-url", () -> "http://localhost:" + scrapperPort);
        registry.add("app.telegram.url", () -> "http://localhost:9999/bot");
    }

    @BeforeEach
    void clearState() {
        scrapperContext.getBean(InMemorySubscriptionRepository.class).clear();
        scrapperContext.getBean(InMemoryTrackedLinkRepository.class).clear();
        scrapperContext.getBean(InMemorySubscriptionTagRepository.class).clear();
        scrapperContext.getBean(InMemoryTelegramChatRepository.class).clear();
        scrapperContext.getBean(InMemoryGithubTrackingStateRepository.class).clear();
        scrapperContext.getBean(InMemoryStackOverflowTrackingStateRepository.class).clear();
    }

    @BeforeEach
    void clearBotState() {
        trackDialogStateRepository.clear();
    }

    @Test
    void shouldSaveLinkAfterTrackDialog() {
        long chatId = 123456L;
        URI link = URI.create("https://github.com/octocat/Hello-World");

        scrapperClient.registerChat(chatId);

        updateService.handleEvent(update(1, chatId, "/track"));
        updateService.handleEvent(update(2, chatId, link.toString()));
        updateService.handleEvent(update(3, chatId, "work, hobby"));


        ListLinksResponse response = scrapperClient.getLinks(chatId);

        assertThat(response).isNotNull();
        assertThat(response.links()).hasSize(1);

        LinkResponse saved = response.links().getFirst();
        assertThat(saved.url()).isEqualTo(link);
        assertThat(saved.tags()).containsExactlyInAnyOrder("work", "hobby");
    }

    @Test
    void shouldNotifyUserWhenTrackLinkIsInvalid() {
        long chatId = 123456L;
        String invalidLink = "jjbj://github.com/user/repo";

        scrapperClient.registerChat(chatId);

        updateService.handleEvent(update(1, chatId, "/track"));
        updateService.handleEvent(update(2, chatId, invalidLink));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(telegramSender, atLeastOnce())
            .sendPlain(eq(chatId), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues())
            .anySatisfy(text -> assertThat(text.toLowerCase()).contains("invalid"));

        ListLinksResponse response = scrapperClient.getLinks(chatId);

        assertThat(response).isNotNull();
        assertThat(response.links()).isEmpty();
        assertThat(response.size()).isZero();
    }

    @Test
    void shouldNotifyUserWhenLinkIsAlreadyTrackedDuringTrackDialog() {
        long chatId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        URI link = URI.create("https://github.com/octocat/Hello-World");

        scrapperClient.registerChat(chatId);

        updateService.handleEvent(update(1, chatId, "/track"));
        updateService.handleEvent(update(2, chatId, link.toString()));
        updateService.handleEvent(update(3, chatId, "work, hobby"));

        ListLinksResponse afterFirstTrack = scrapperClient.getLinks(chatId);
        assertThat(afterFirstTrack).isNotNull();
        assertThat(afterFirstTrack.links()).hasSize(1);

        Mockito.clearInvocations(telegramSender);

        updateService.handleEvent(update(4, chatId, "/track"));
        updateService.handleEvent(update(5, chatId, link.toString()));
        updateService.handleEvent(update(6, chatId, "work, hobby"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(telegramSender, atLeastOnce())
            .sendPlain(eq(chatId), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues())
            .anySatisfy(text -> assertThat(text.toLowerCase())
                .contains("followed"));

        ListLinksResponse afterSecondTrack = scrapperClient.getLinks(chatId);
        assertThat(afterSecondTrack).isNotNull();
        assertThat(afterSecondTrack.links()).hasSize(1);

        LinkResponse saved = afterSecondTrack.links().getFirst();
        assertThat(saved.url()).isEqualTo(link);
    }

    @Test
    void shouldSendActiveSubscriptionsListWhenUserRequestsList() {
        long chatId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);

        URI firstLink = URI.create("https://github.com/octocat/Hello-World");
        URI secondLink = URI.create("https://github.com/spring-projects/spring-boot");

        scrapperClient.registerChat(chatId);

        scrapperClient.addLink(chatId, new AddLinkRequest(firstLink, Set.of("work"), List.of()));
        scrapperClient.addLink(chatId, new AddLinkRequest(secondLink, Set.of("study"), List.of()));

        ListLinksResponse storedLinks = scrapperClient.getLinks(chatId);
        assertThat(storedLinks).isNotNull();
        assertThat(storedLinks.links()).hasSize(2);

        Mockito.clearInvocations(telegramSender);

        updateService.handleEvent(update(1, chatId, "/list"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(telegramSender, atLeastOnce())
            .sendPlain(eq(chatId), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues())
            .anySatisfy(text -> {
                String normalized = text.toLowerCase();
                assertThat(normalized).contains(firstLink.toString().toLowerCase());
                assertThat(normalized).contains(secondLink.toString().toLowerCase());
            });
    }

    @Test
    void shouldSendNoActiveSubscriptionsMessageWhenUserRequestsListAndHasNoSubscriptions() {
        long chatId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);

        scrapperClient.registerChat(chatId);

        ListLinksResponse storedLinks = scrapperClient.getLinks(chatId);
        assertThat(storedLinks).isNotNull();
        assertThat(storedLinks.links()).isEmpty();
        assertThat(storedLinks.size()).isZero();

        Mockito.clearInvocations(telegramSender);

        updateService.handleEvent(update(1, chatId, "/list"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(telegramSender, atLeastOnce())
            .sendPlain(eq(chatId), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues())
            .anySatisfy(text -> assertThat(text.toLowerCase())
                .contains("empty"));
    }

    @Test
    void shouldSendOnlySubscriptionsWithRequestedTagWhenUserRequestsListByTag() {
        long chatId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);

        URI workLink1 = URI.create("https://github.com/octocat/Hello-World");
        URI workLink2 = URI.create("https://github.com/spring-projects/spring-boot");
        URI studyLink = URI.create("https://github.com/openjdk/jdk");

        scrapperClient.registerChat(chatId);

        scrapperClient.addLink(chatId, new AddLinkRequest(workLink1, Set.of("work"), List.of()));
        scrapperClient.addLink(chatId, new AddLinkRequest(workLink2, Set.of("backend", "work"), List.of()));
        scrapperClient.addLink(chatId, new AddLinkRequest(studyLink, Set.of("study"), List.of()));

        ListLinksResponse storedLinks = scrapperClient.getLinks(chatId);
        assertThat(storedLinks).isNotNull();
        assertThat(storedLinks.links()).hasSize(3);

        Mockito.clearInvocations(telegramSender);

        updateService.handleEvent(update(1, chatId, "/list work"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(telegramSender, atLeastOnce())
            .sendPlain(eq(chatId), messageCaptor.capture());

        assertThat(messageCaptor.getAllValues())
            .anySatisfy(text -> {
                String normalized = text.toLowerCase();

                assertThat(normalized).contains(workLink1.toString().toLowerCase());
                assertThat(normalized).contains(workLink2.toString().toLowerCase());
                assertThat(normalized).doesNotContain(studyLink.toString().toLowerCase());
            });
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

    @Configuration
    static class ScrapperTestConfig {

        @Bean(name = "gitHubClient")
        @Primary
        GitHubClient gitHubClient() {
            GitHubClient client = Mockito.mock(GitHubClient.class);
            GitHubRepositoryFetchResult fetchResult = Mockito.mock(GitHubRepositoryFetchResult.class);

            when(fetchResult.isOk()).thenReturn(true);
            when(fetchResult.isNotModified()).thenReturn(false);
            when(fetchResult.etag()).thenReturn("\"test-etag\"");
            when(fetchResult.statusCode()).thenReturn(HttpStatusCode.valueOf(200));

            when(client.fetchRepository(any(GitHubRepositoryKey.class), Mockito.nullable(String.class)))
                .thenReturn(fetchResult);

            when(client.fetchRecentActivities(any(GitHubRepositoryKey.class), anyInt()))
                .thenReturn(List.of());

            return client;
        }
    }
}
