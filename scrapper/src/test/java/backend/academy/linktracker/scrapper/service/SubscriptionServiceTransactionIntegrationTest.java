package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.common.PreparedTrackingState;
import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.integration.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class SubscriptionServiceTransactionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @MockitoBean
    private LinkHandlerRegistry linkHandlerRegistry;

    @MockitoBean
    private SubscriptionTagRepository subscriptionTagRepository;

    @Test
    void shouldRollbackTrackedLinkCreationWhenSubscriptionPersistenceFails() {
        telegramChatRepository.save(new TelegramChat(1L));

        URI uri = URI.create("https://github.com/octocat/Hello-World");
        LinkHandler handler = mock(LinkHandler.class);

        when(linkHandlerRegistry.getHandler(uri)).thenReturn(handler);
        when(handler.parse(uri)).thenReturn(new ParsedLink(uri.toString(), new GitHubRepositoryKey("octocat", "Hello-World")));
        when(handler.prepareTrackingState(any(ParsedLink.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return (PreparedTrackingState) trackedLink -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            };
        });
        doThrow(new IllegalStateException("Subscription tags are unavailable"))
                .when(subscriptionTagRepository)
                .addTags(any(Subscription.class), anySet());

        AddLinkRequest request = new AddLinkRequest(uri, Set.of("java"), List.of());

        assertThatThrownBy(() -> subscriptionService.addSubscription(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tags are unavailable");

        assertThat(trackedLinkRepository.findAll()).isEmpty();
        assertThat(subscriptionRepository.findAllByTelegramChatId(1L)).isEmpty();
    }
}
