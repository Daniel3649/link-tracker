package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.link.ParsedLink;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkHandlerRegistry handlerRegistry;
    private final TrackedLinkRepository trackedLinkRepository;
    private final TelegramChatRepository telegramChatRepository;
    private final SubscriptionService subscriptionService;

    private final ConcurrentMap<ResourceKey, Object> linkCreationLocks = new ConcurrentHashMap<>();

    public void addLink(long chatId, AddLinkRequest request) {
        TelegramChat telegramChat = telegramChatRepository.findByChatId(chatId)
            .orElseThrow(() -> new TelegramChatNotFoundException("Chat not found. Id: " + chatId));

        LinkHandler handler = handlerRegistry.getHandler(request.link());
        ParsedLink parsedLink = handler.parse(request.link());

        TrackedLink trackedLink = getOrCreateTrackedLink(handler, parsedLink);

        subscriptionService.createSubscription(trackedLink, telegramChat, request.tags());
    }

    private TrackedLink getOrCreateTrackedLink(LinkHandler handler, ParsedLink parsedLink) {
        ResourceKey resourceKey = parsedLink.resourceKey();
        Object lock = linkCreationLocks.computeIfAbsent(resourceKey, _ -> new Object());

        synchronized (lock) {
            return trackedLinkRepository.findByResourceKey(resourceKey)
                .orElseGet(() -> createTrackedLink(handler, parsedLink));
        }
    }

    private TrackedLink createTrackedLink(LinkHandler handler, ParsedLink parsedLink) {
        TrackedLink savedTrackedLink = trackedLinkRepository.save(
            new TrackedLink(
                null,
                parsedLink.url(),
                parsedLink.resourceKey()
            )
        );

        try {
            handler.createTrackingState(savedTrackedLink);
            return savedTrackedLink;
        } catch (RuntimeException e) {
            trackedLinkRepository.deleteByResourceKey(parsedLink.resourceKey());
            throw e;
        }
    }
}
