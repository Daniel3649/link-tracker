package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.common.PreparedTrackedLink;
import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.exception.link.NotFoundTrackedLinkException;
import backend.academy.linktracker.scrapper.mapper.SubscriptionMapper;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.service.LinkService;
import backend.academy.linktracker.scrapper.service.SubscriptionService;
import backend.academy.linktracker.scrapper.service.persistence.SubscriptionPersistenceService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private TelegramChatRepository telegramChatRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private LinkService linkService;

    @Mock
    private SubscriptionPersistenceService subscriptionPersistenceService;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void shouldRetrySubscriptionCreationWhenTrackedLinkDisappearsAfterLookup() {
        URI uri = URI.create("https://github.com/octocat/Hello-World");
        GitHubRepositoryKey resourceKey = new GitHubRepositoryKey("octocat", "Hello-World");
        AddLinkRequest request = new AddLinkRequest(uri, Set.of("Java"), List.of());
        TelegramChat telegramChat = new TelegramChat(1L);
        TrackedLink staleTrackedLink = new TrackedLink(10L, uri.toString(), resourceKey);
        PreparedTrackedLink preparedTrackedLink =
                new PreparedTrackedLink(new ParsedLink(uri.toString(), resourceKey), trackedLink -> {});
        TrackedLink recreatedTrackedLink = new TrackedLink(20L, uri.toString(), resourceKey);
        Subscription savedSubscription = new Subscription(30L, recreatedTrackedLink, telegramChat);
        LinkResponse expectedResponse = new LinkResponse(30L, uri, List.of("Java"), List.of());

        when(telegramChatRepository.findByChatId(1L)).thenReturn(Optional.of(telegramChat));
        when(linkService.findTrackedLink(uri)).thenReturn(Optional.of(staleTrackedLink));
        when(subscriptionPersistenceService.createSubscription(staleTrackedLink, telegramChat, Set.of("Java")))
                .thenThrow(new NotFoundTrackedLinkException("Tracked link not found: " + uri));
        when(linkService.prepareTrackedLink(uri)).thenReturn(preparedTrackedLink);
        when(subscriptionPersistenceService.createSubscription(preparedTrackedLink, telegramChat, Set.of("Java")))
                .thenReturn(savedSubscription);
        when(subscriptionMapper.toLinkResponse(savedSubscription)).thenReturn(expectedResponse);

        LinkResponse actualResponse = subscriptionService.addSubscription(1L, request);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(subscriptionPersistenceService).createSubscription(staleTrackedLink, telegramChat, Set.of("Java"));
        verify(linkService).prepareTrackedLink(uri);
        verify(subscriptionPersistenceService).createSubscription(preparedTrackedLink, telegramChat, Set.of("Java"));
    }
}
