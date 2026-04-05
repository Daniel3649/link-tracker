package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.integration.AbstractIntegrationTest;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class SubscriptionTagNormalizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @MockitoBean
    private LinkHandlerRegistry linkHandlerRegistry;

    @Test
    void shouldNormalizeAndDeduplicateTagsWhenAddingSubscription() {
        telegramChatRepository.save(new TelegramChat(1L));

        URI uri = URI.create("https://github.com/octocat/Hello-World");
        LinkHandler handler = mock(LinkHandler.class);

        when(linkHandlerRegistry.getHandler(uri)).thenReturn(handler);
        when(handler.parse(uri))
                .thenReturn(new ParsedLink(uri.toString(), new GitHubRepositoryKey("octocat", "Hello-World")));
        when(handler.prepareTrackingState(any(ParsedLink.class))).thenReturn(trackedLink -> {});

        AddLinkRequest request = new AddLinkRequest(uri, Set.of("java", " java ", "spring "), List.of());
        subscriptionService.addSubscription(1L, request);

        ListLinksResponse response = subscriptionService.getAllSubscriptions(1L);

        assertThat(response.size()).isEqualTo(1);
        assertThat(response.links().getFirst().tags()).containsExactly("java", "spring");
    }
}
