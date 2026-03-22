package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, Long> {
    boolean existsByTrackedLink_IdAndTelegramChat_Id(Long trackedLinkId, Long telegramChatId);

    boolean existsByTrackedLink_Id(Long trackedLinkId);

    Optional<SubscriptionEntity> findByTrackedLink_IdAndTelegramChat_Id(Long trackedLinkId, Long telegramChatId);

    void deleteByTrackedLink_IdAndTelegramChat_Id(Long trackedLinkId, Long telegramChatId);

    List<SubscriptionEntity> findAllByTelegramChat_IdOrderByIdAsc(Long telegramChatId);

    List<SubscriptionEntity> findAllByTrackedLink_IdOrderByIdAsc(Long trackedLinkId);
}
