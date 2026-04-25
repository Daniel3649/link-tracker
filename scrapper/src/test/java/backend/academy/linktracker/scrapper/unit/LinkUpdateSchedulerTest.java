package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
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

        TelegramChat chat1 = mock(TelegramChat.class);
        when(chat1.getId()).thenReturn(101L);

        TelegramChat chat2 = mock(TelegramChat.class);
        when(chat2.getId()).thenReturn(202L);

        Subscription subscription1 = mock(Subscription.class);
        when(subscription1.getTelegramChat()).thenReturn(chat1);

        Subscription subscription2 = mock(Subscription.class);
        when(subscription2.getTelegramChat()).thenReturn(chat2);

        when(trackedLinkRepository.findAll()).thenReturn(List.of(trackedLink));
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
        when(trackedLink.getUrl()).thenReturn(url);

        LinkChange change = new LinkChange("New commit detected");

        when(trackedLinkRepository.findAll()).thenReturn(List.of(trackedLink));
        when(linkHandlerRegistry.getHandler(uri)).thenReturn(linkHandler);
        when(linkHandler.checkForUpdate(trackedLink)).thenReturn(Optional.of(change));
        when(subscriptionRepository.findAllByTrackedLink(trackedLink)).thenReturn(List.of());

        scheduler.checkUpdates();

        verify(linkUpdateSender, never()).send(any(LinkUpdate.class));
    }
}
