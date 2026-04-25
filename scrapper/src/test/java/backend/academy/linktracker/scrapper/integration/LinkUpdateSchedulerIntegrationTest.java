package backend.academy.linktracker.scrapper.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.schedule.LinkUpdateScheduler;
import backend.academy.linktracker.scrapper.sender.LinkUpdateSender;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class LinkUpdateSchedulerIntegrationTest {

    @Autowired
    private LinkUpdateScheduler scheduler;

    @Autowired
    private TelegramChatRepository telegramChatRepository;

    @Autowired
    private TrackedLinkRepository trackedLinkRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionTagRepository subscriptionTagRepository;

    @Autowired
    private GitHubTrackingStateRepository gitHubTrackingStateRepository;

    @Autowired
    private StackOverflowTrackingStateRepository stackOverflowTrackingStateRepository;

    @MockitoBean
    private LinkHandlerRegistry linkHandlerRegistry;

    @MockitoBean
    private LinkUpdateSender linkUpdateSender;

    @BeforeEach
    void cleanRepositories() {
        telegramChatRepository.clear();
        trackedLinkRepository.clear();
        subscriptionRepository.clear();
        subscriptionTagRepository.clear();
        gitHubTrackingStateRepository.clear();
        stackOverflowTrackingStateRepository.clear();
    }

    @Test
    void shouldSendUpdateOnlyToSubscribedChats() {
        TelegramChat chat1 = telegramChatRepository.save(new TelegramChat(1L));
        TelegramChat chat2 = telegramChatRepository.save(new TelegramChat(2L));
        telegramChatRepository.save(new TelegramChat(999L));

        TrackedLink trackedLink = trackedLinkRepository.save(new TrackedLink(
                null, "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World")));

        subscriptionRepository.save(new Subscription(null, trackedLink, chat1));
        subscriptionRepository.save(new Subscription(null, trackedLink, chat2));

        LinkHandler handler = mock(LinkHandler.class);
        when(linkHandlerRegistry.getHandler(any(URI.class))).thenReturn(handler);
        when(handler.checkForUpdate(trackedLink)).thenReturn(Optional.of(new LinkChange("Repository changed")));

        scheduler.checkUpdates();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(linkUpdateSender, times(1)).send(captor.capture());

        LinkUpdate update = captor.getValue();
        assertThat(update.id()).isEqualTo(trackedLink.getId());
        assertThat(update.url()).isEqualTo(URI.create("https://github.com/octocat/Hello-World"));
        assertThat(update.tgChatIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(update.tgChatIds()).doesNotContain(999L);
    }

    @Test
    void shouldNotSendUpdateWhenThereAreNoSubscribers() {
        TrackedLink trackedLink = trackedLinkRepository.save(new TrackedLink(
                null, "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World")));

        LinkHandler handler = mock(LinkHandler.class);
        when(linkHandlerRegistry.getHandler(any(URI.class))).thenReturn(handler);
        when(handler.checkForUpdate(trackedLink)).thenReturn(Optional.of(new LinkChange("Repository changed")));

        scheduler.checkUpdates();

        verify(linkUpdateSender, never()).send(any());
    }
}
