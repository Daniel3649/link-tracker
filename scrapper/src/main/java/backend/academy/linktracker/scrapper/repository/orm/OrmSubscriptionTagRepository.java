package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.SubscriptionTagJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import java.util.LinkedHashSet;
import java.util.Set;

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

        repository.flush();
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
        entityManager.createNativeQuery("truncate table subscription_tag cascade").executeUpdate();
    }
}
