package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionTagJpaRepository extends JpaRepository<SubscriptionTagEntity, SubscriptionTagId> {
    List<SubscriptionTagEntity> findAllBySubscriptionIdOrderByIdTagAsc(Long subscriptionId);

    boolean existsBySubscriptionIdAndIdTag(Long subscriptionId, String tag);

    int deleteBySubscriptionIdAndIdTag(Long subscriptionId, String tag);

    void deleteAllBySubscriptionId(Long subscriptionId);
}
