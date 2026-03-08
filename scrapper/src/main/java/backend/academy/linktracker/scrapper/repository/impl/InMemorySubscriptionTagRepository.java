package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import org.springframework.stereotype.Repository;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemorySubscriptionTagRepository implements SubscriptionTagRepository {
    private final ConcurrentMap<Subscription, Set<String>> tags = new ConcurrentHashMap<>();

    @Override
    public void addTags(Subscription subscription, Set<String> subscriptionTags) {
        if (subscriptionTags == null || subscriptionTags.isEmpty()) {
            return;
        }

        tags.computeIfAbsent(subscription, ignored -> ConcurrentHashMap.newKeySet())
            .addAll(Set.copyOf(subscriptionTags));
    }

    @Override
    public void deleteAllBySubscription(Subscription subscription) {
        tags.remove(subscription);
    }

}
