package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionTagJpaRepository extends JpaRepository<SubscriptionTagEntity, SubscriptionTagId> {
    List<SubscriptionTagEntity> findAllBySubscription_IdOrderById_TagAsc(Long subscriptionId);

    void deleteAllBySubscription_Id(Long subscriptionId);
}
