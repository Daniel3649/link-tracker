package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.schedule.LinkUpdateScheduler;
import backend.academy.linktracker.scrapper.sender.LinkUpdateSender;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateSchedulerTest {

    @Mock
    private TrackedLinkRepository trackedLinkRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private LinkHandlerRegistry linkHandlerRegistry;

    @Mock
    private LinkUpdateSender linkUpdateSender;

    @Mock
    private LinkHandler linkHandler;

    @Mock
    private SchedulerProperties schedulerProperties;

    @InjectMocks
    private LinkUpdateScheduler scheduler;

    @Test
    void shouldSendUpdateOnlyToUsersWhoTrackThisLink() {
        String url = "https://github.com/octocat/Hello-World";
        URI uri = URI.create(url);

        TrackedLink trackedLink = mock(TrackedLink.class);
        when(trackedLink.getId()).thenReturn(10L);
        when(trackedLink.getUrl()).thenReturn(url);

        LinkChange change = new LinkChange("New commit detected");

        TelegramChat chat1 = new TelegramChat(101L);
        TelegramChat chat2 = new TelegramChat(202L);

        Subscription subscription1 = new Subscription(1L, trackedLink, chat1);
        Subscription subscription2 = new Subscription(2L, trackedLink, chat2);

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100)).thenReturn(List.of(trackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(uri)).thenReturn(linkHandler);
        when(linkHandler.checkForUpdate(trackedLink)).thenReturn(Optional.of(change));
        when(subscriptionRepository.findAllByTrackedLink(trackedLink))
                .thenReturn(List.of(subscription1, subscription2));

        scheduler.checkUpdates();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(linkUpdateSender).send(captor.capture());

        LinkUpdate sentUpdate = captor.getValue();

        assertThat(sentUpdate.id()).isEqualTo(10L);
        assertThat(sentUpdate.url()).isEqualTo(uri);
        assertThat(sentUpdate.description()).isEqualTo("New commit detected");
        assertThat(sentUpdate.tgChatIds()).containsExactly(101L, 202L);
        assertThat(sentUpdate.tgChatIds()).doesNotContain(303L);
    }

    @Test
    void shouldNotSendUpdateWhenNobodyTracksLink() {
        String url = "https://github.com/octocat/Hello-World";
        URI uri = URI.create(url);

        TrackedLink trackedLink = mock(TrackedLink.class);
        when(trackedLink.getId()).thenReturn(10L);
        when(trackedLink.getUrl()).thenReturn(url);

        LinkChange change = new LinkChange("New commit detected");

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100)).thenReturn(List.of(trackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(uri)).thenReturn(linkHandler);
        when(linkHandler.checkForUpdate(trackedLink)).thenReturn(Optional.of(change));
        when(subscriptionRepository.findAllByTrackedLink(trackedLink)).thenReturn(List.of());

        scheduler.checkUpdates();

        verify(linkUpdateSender, never()).send(any(LinkUpdate.class));
    }

    @Test
    void shouldProcessAllTrackedLinksAcrossMultipleBatches() {
        String firstUrl = "https://github.com/octocat/Hello-World";
        String secondUrl = "https://github.com/octocat/Spoon-Knife";

        TrackedLink firstTrackedLink = mock(TrackedLink.class);
        when(firstTrackedLink.getId()).thenReturn(10L);
        when(firstTrackedLink.getUrl()).thenReturn(firstUrl);

        TrackedLink secondTrackedLink = mock(TrackedLink.class);
        when(secondTrackedLink.getId()).thenReturn(20L);
        when(secondTrackedLink.getUrl()).thenReturn(secondUrl);

        LinkHandler firstHandler = mock(LinkHandler.class);
        LinkHandler secondHandler = mock(LinkHandler.class);

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(1);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 1)).thenReturn(List.of(firstTrackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 1)).thenReturn(List.of(secondTrackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(20L, 1)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(URI.create(firstUrl))).thenReturn(firstHandler);
        when(linkHandlerRegistry.getHandler(URI.create(secondUrl))).thenReturn(secondHandler);
        when(firstHandler.checkForUpdate(firstTrackedLink)).thenReturn(Optional.empty());
        when(secondHandler.checkForUpdate(secondTrackedLink)).thenReturn(Optional.empty());

        scheduler.checkUpdates();

        verify(firstHandler).checkForUpdate(firstTrackedLink);
        verify(secondHandler).checkForUpdate(secondTrackedLink);
        verify(linkUpdateSender, never()).send(any(LinkUpdate.class));
    }
}
