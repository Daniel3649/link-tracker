package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import java.util.Set;

public interface SubscriptionTagRepository {
    void addTags(Subscription subscription, Set<String> tags);
}
