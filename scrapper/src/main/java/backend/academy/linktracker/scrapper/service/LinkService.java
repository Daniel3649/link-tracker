package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.link.ParsedLink;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkHandlerRegistry handlerRegistry;
    private final TrackedLinkRepository trackedLinkRepository;
    private final TelegramChatRepository telegramChatRepository;
    private final SubscriptionService subscriptionService;

    public void addLink(long chatId, String url, Set<String> tags) {
        TelegramChat telegramChat = telegramChatRepository.findByChatId(chatId)
            .orElseThrow(() -> new TelegramChatNotFoundException("Chat not found. Id: " + chatId));

        URI uri = normalize(url);

        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);

        TrackedLink trackedLink = trackedLinkRepository.findByResourceKey(parsedLink.resourceKey())
            .orElseGet(() -> {
                TrackedLink newLink = trackedLinkRepository.save(
                    new TrackedLink(null, parsedLink.url(), parsedLink.resourceKey())
                );

                handler.createTrackingState(newLink);
                return newLink;
            });

        subscriptionService.createSubscription(trackedLink, telegramChat, tags);
    }

    private URI normalize(String url) {
        try {
            return URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new UnsupportedLinkException("Incorrect URL: " + url, e);
        }
    }
}
