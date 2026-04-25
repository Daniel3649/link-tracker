package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.StackOverflowTrackingStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StackOverflowTrackingStateJpaRepository extends JpaRepository<StackOverflowTrackingStateEntity, Long> {
    void deleteByTrackedLinkId(Long trackedLinkId);
}
