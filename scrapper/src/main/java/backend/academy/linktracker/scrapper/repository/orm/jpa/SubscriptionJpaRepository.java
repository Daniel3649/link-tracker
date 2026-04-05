package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, Long> {
    boolean existsByTrackedLinkIdAndTelegramChatChatId(Long trackedLinkId, Long telegramChatId);

    boolean existsByTrackedLinkId(Long trackedLinkId);

    Optional<SubscriptionEntity> findByTrackedLinkIdAndTelegramChatChatId(Long trackedLinkId, Long telegramChatId);

    void deleteByTrackedLinkIdAndTelegramChatChatId(Long trackedLinkId, Long telegramChatId);

    List<SubscriptionEntity> findAllByTelegramChatChatIdOrderByIdAsc(Long telegramChatId);

    List<SubscriptionEntity> findAllByTrackedLinkIdOrderByIdAsc(Long trackedLinkId);

    @Query("select s.chatId from SubscriptionEntity s where s.linkId = :trackedLinkId order by s.id")
    List<Long> findAllChatIdsByTrackedLinkIdOrderByIdAsc(Long trackedLinkId);
}
