package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.request.AddLinkRequest;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final TelegramChatRepository telegramChatRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTagRepository subscriptionTagRepository;
    private final LinkService linkService;

    private final ConcurrentMap<MapKey, Object> subscriptionCreationLocks = new ConcurrentHashMap<>();
    private record MapKey(TrackedLink trackedLink, TelegramChat telegramChat) {}

    public void addSubscription(long chatId, AddLinkRequest request) {
        TelegramChat telegramChat = telegramChatRepository.findByChatId(chatId)
            .orElseThrow(() -> new TelegramChatNotFoundException("Chat not found. Id: " + chatId));
        TrackedLink trackedLink = linkService.getOrCreateTrackedLink(request.link());
        createSubscription(trackedLink, telegramChat, request.tags());
    }

    private void createSubscription(TrackedLink trackedLink, TelegramChat telegramChat, Set<String> tags) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        Object lock = subscriptionCreationLocks.computeIfAbsent(mapKey, ignored -> new Object());

        synchronized (lock) {
            if (subscriptionRepository.existsByTrackedLinkAndTelegramChat(trackedLink, telegramChat)) {
                throw new SubscriptionAlreadyExistsException("Link is already tracked: " + trackedLink.getUrl());
            }

            Subscription savedSubscription = subscriptionRepository.save(
                new Subscription(null, trackedLink, telegramChat)
            );

            subscriptionTagRepository.addTags(savedSubscription, tags);
        }
    }
}
