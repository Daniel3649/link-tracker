package backend.academy.linktracker.scrapper.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.contract.dto.request.TextNotification;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangeSource;
import backend.academy.linktracker.scrapper.common.LinkChangeType;
import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.integration.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.sender.LinkUpdateSender;
import backend.academy.linktracker.scrapper.sender.TextNotificationSender;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class LinkUpdateSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LinkUpdateScheduler scheduler;

    @MockitoBean
    private LinkHandlerRegistry linkHandlerRegistry;

    @MockitoBean
    private LinkUpdateSender linkUpdateSender;

    @MockitoBean
    private TextNotificationSender textNotificationSender;

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

    @Test
    void shouldFormatStructuredChangeDescriptionBeforeSendingNotification() {
        TelegramChat chat = telegramChatRepository.save(new TelegramChat(1L));
        TrackedLink trackedLink = trackedLinkRepository.save(new TrackedLink(
                null, "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World")));
        subscriptionRepository.save(new Subscription(null, trackedLink, chat));

        LinkHandler handler = mock(LinkHandler.class);
        when(linkHandlerRegistry.getHandler(any(URI.class))).thenReturn(handler);
        when(handler.checkForUpdate(trackedLink))
                .thenReturn(Optional.of(new LinkChange(
                        "New GitHub issue",
                        LinkChangeSource.GITHUB,
                        LinkChangeType.GITHUB_ISSUE,
                        "Fix login flow",
                        "alice",
                        Instant.parse("2026-04-05T08:30:00Z"),
                        "Preview text")));

        scheduler.checkUpdates();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(linkUpdateSender).send(captor.capture());

        assertThat(captor.getValue().description())
                .isEqualTo(
                        """
                        New GitHub issue
                        Title: Fix login flow
                        User: alice
                        Created at: 2026-04-05T08:30:00Z
                        Preview: Preview text""");
    }

    @Test
    void shouldContinueProcessingOtherLinksWhenOnePollingFails() {
        TelegramChat chat = telegramChatRepository.save(new TelegramChat(1L));

        TrackedLink failedTrackedLink = trackedLinkRepository.save(new TrackedLink(
                null, "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World")));
        TrackedLink successfulTrackedLink = trackedLinkRepository.save(new TrackedLink(
                null,
                "https://github.com/octocat/Spoon-Knife",
                new GitHubRepositoryKey("octocat", "Spoon-Knife")));

        subscriptionRepository.save(new Subscription(null, failedTrackedLink, chat));
        subscriptionRepository.save(new Subscription(null, successfulTrackedLink, chat));

        LinkHandler failedHandler = mock(LinkHandler.class);
        LinkHandler successfulHandler = mock(LinkHandler.class);
        when(linkHandlerRegistry.getHandler(URI.create(failedTrackedLink.getUrl()))).thenReturn(failedHandler);
        when(linkHandlerRegistry.getHandler(URI.create(successfulTrackedLink.getUrl()))).thenReturn(successfulHandler);
        when(failedHandler.checkForUpdate(failedTrackedLink))
                .thenThrow(new RepositoryPollingException("GitHub API unavailable"));
        when(successfulHandler.checkForUpdate(successfulTrackedLink))
                .thenReturn(Optional.of(new LinkChange("Repository changed")));

        scheduler.checkUpdates();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(linkUpdateSender, times(1)).send(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(successfulTrackedLink.getId());

        ArgumentCaptor<TextNotification> reportCaptor = ArgumentCaptor.forClass(TextNotification.class);
        verify(textNotificationSender, times(1)).send(reportCaptor.capture());
        assertThat(reportCaptor.getValue().tgChatIds()).containsExactly(1L);
        assertThat(reportCaptor.getValue().message())
                .contains("Link check report")
                .contains(failedTrackedLink.getUrl())
                .contains("GitHub API unavailable")
                .doesNotContain(successfulTrackedLink.getUrl());
    }
}
