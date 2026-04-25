package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import java.util.Set;

public interface SubscriptionTagRepository {
    void addTags(Subscription subscription, Set<String> tags);

    boolean addTag(Subscription subscription, String tag);

    boolean existsBySubscriptionAndTag(Subscription subscription, String tag);

    boolean updateTag(Subscription subscription, String currentTag, String newTag);

    boolean deleteTag(Subscription subscription, String tag);

    void deleteAllBySubscription(Subscription subscription);

    Set<String> findAllBySubscription(Subscription subscription);

    void clear();
}
