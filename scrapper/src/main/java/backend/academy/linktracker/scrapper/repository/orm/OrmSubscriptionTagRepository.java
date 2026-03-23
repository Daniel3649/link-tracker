package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.SubscriptionTagJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

@RequiredArgsConstructor
public class OrmSubscriptionTagRepository implements SubscriptionTagRepository {
    private final SubscriptionTagJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public void addTags(Subscription subscription, Set<String> subscriptionTags) {
        if (subscriptionTags == null || subscriptionTags.isEmpty()) {
            return;
        }

        SubscriptionEntity subscriptionEntity =
                entityManager.getReference(SubscriptionEntity.class, subscription.getId());

        subscriptionTags.stream()
                .distinct()
                .forEach(tag -> repository.save(new SubscriptionTagEntity(subscriptionEntity, tag)));
    }

    @Override
    public boolean addTag(Subscription subscription, String tag) {
        SubscriptionEntity subscriptionEntity =
                entityManager.getReference(SubscriptionEntity.class, subscription.getId());

        try {
            repository.save(new SubscriptionTagEntity(subscriptionEntity, tag));
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    @Override
    public boolean existsBySubscriptionAndTag(Subscription subscription, String tag) {
        return repository.existsBySubscription_IdAndId_Tag(subscription.getId(), tag);
    }

    @Override
    public boolean updateTag(Subscription subscription, String currentTag, String newTag) {
        return entityManager
                        .createNativeQuery("""
                                update subscription_tag
                                set tag = :newTag
                                where subscription_id = :subscriptionId and tag = :currentTag
                                """)
                        .setParameter("subscriptionId", subscription.getId())
                        .setParameter("currentTag", currentTag)
                        .setParameter("newTag", newTag)
                        .executeUpdate()
                > 0;
    }

    @Override
    public boolean deleteTag(Subscription subscription, String tag) {
        return repository.deleteBySubscription_IdAndId_Tag(subscription.getId(), tag) > 0;
    }

    @Override
    public void deleteAllBySubscription(Subscription subscription) {
        repository.deleteAllBySubscription_Id(subscription.getId());
    }

    @Override
    public Set<String> findAllBySubscription(Subscription subscription) {
        return repository.findAllBySubscription_IdOrderById_TagAsc(subscription.getId()).stream()
                .map(SubscriptionTagEntity::getTag)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void clear() {
        entityManager
                .createNativeQuery("truncate table subscription_tag cascade")
                .executeUpdate();
    }
}
