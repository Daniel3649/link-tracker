package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public class InMemorySubscriptionRepository implements SubscriptionRepository {
    private record MapKey(TrackedLink trackedLink, TelegramChat telegramChat) {}

    private final AtomicLong idSequence = new AtomicLong();
    private final ConcurrentMap<MapKey, Subscription> subscriptions = new ConcurrentHashMap<>();

    @Override
    public boolean existsByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        return subscriptions.containsKey(mapKey);
    }

    @Override
    public Subscription save(Subscription subscription) {
        MapKey key = new MapKey(subscription.getTrackedLink(), subscription.getTelegramChat());

        if (subscription.getId() != null) {
            subscriptions.put(key, subscription);
            return subscription;
        }

        Subscription newSubscription = new Subscription(
                idSequence.incrementAndGet(), subscription.getTrackedLink(), subscription.getTelegramChat());

        Subscription existing = subscriptions.putIfAbsent(key, newSubscription);
        return existing != null ? existing : newSubscription;
    }

    @Override
    public boolean existsByTrackedLink(TrackedLink trackedLink) {
        return subscriptions.keySet().stream().anyMatch(key -> key.trackedLink().equals(trackedLink));
    }

    @Override
    public Optional<Subscription> findByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        return Optional.ofNullable(subscriptions.get(mapKey));
    }

    @Override
    public void deleteByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        subscriptions.remove(mapKey);
    }

    @Override
    public List<Subscription> findAllByTelegramChat(TelegramChat telegramChat) {
        return subscriptions.values().stream()
                .filter(subscription -> Objects.equals(subscription.getTelegramChat(), telegramChat))
                .toList();
    }

    @Override
    public List<Subscription> findAllByTrackedLink(TrackedLink trackedLink) {
        return subscriptions.values().stream()
                .filter(subscription -> Objects.equals(subscription.getTrackedLink(), trackedLink))
                .toList();
    }

    @Override
    public void clear() {
        subscriptions.clear();
    }
}
