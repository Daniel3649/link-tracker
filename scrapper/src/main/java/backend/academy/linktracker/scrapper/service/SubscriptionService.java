package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTagRepository subscriptionTagRepository;

    private final ConcurrentMap<MapKey, Object> subscriptionCreationLocks = new ConcurrentHashMap<>();
    private record MapKey(TrackedLink trackedLink, TelegramChat telegramChat) {}

    public void createSubscription(TrackedLink trackedLink, TelegramChat telegramChat, Set<String> tags) {
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
