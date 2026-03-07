package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemorySubscriptionRepository implements SubscriptionRepository {
    private record MapKey(TrackedLink link, TelegramChat chat) {}

    private final AtomicLong idSequence = new AtomicLong();

    private final ConcurrentMap<MapKey, Subscription>  subscriptions = new ConcurrentHashMap<>();

    @Override
    public boolean existsByResourceKeyAndChatId(TrackedLink trackedLink, TelegramChat telegramChat) {
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

        long id = idSequence.incrementAndGet();
        Subscription newSubscription = new Subscription(id, subscription.getTrackedLink(),
            subscription.getTelegramChat());
        subscriptions.put(key, newSubscription);
        return newSubscription;
    }
}
