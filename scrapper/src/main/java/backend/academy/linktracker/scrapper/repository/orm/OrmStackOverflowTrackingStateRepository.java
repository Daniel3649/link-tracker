package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.cursor.StackOverflowTimelineCursor;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.StackOverflowTrackingStateEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.StackOverflowTrackingStateJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class OrmStackOverflowTrackingStateRepository implements StackOverflowTrackingStateRepository {
    private final StackOverflowTrackingStateJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public StackOverflowTrackingState save(StackOverflowTrackingState stackOverflowTrackingState) {
        Long trackedLinkId = stackOverflowTrackingState.getTrackedLink().getId();
        StackOverflowTrackingStateEntity entity = repository
                .findById(trackedLinkId)
                .orElseGet(() -> {
                    StackOverflowTrackingStateEntity newEntity = new StackOverflowTrackingStateEntity();
                    newEntity.setTrackedLink(entityManager.getReference(TrackedLinkEntity.class, trackedLinkId));
                    return newEntity;
                });

        StackOverflowTimelineCursor cursor = stackOverflowTrackingState.getTimelineCursor();
        entity.setLastCreationDateEpochSec(cursor != null ? cursor.lastCreationDateEpochSec() : 0L);
        entity.setLastEventKey(cursor != null ? cursor.lastEventKey() : null);
        entity.setNextCheckAt(stackOverflowTrackingState.getNextCheckAt());
        entity.setLastQuestionActivityDateEpochSec(stackOverflowTrackingState.getLastQuestionActivityDateEpochSec());

        return toDomain(repository.saveAndFlush(entity), stackOverflowTrackingState.getTrackedLink());
    }

    @Override
    public void deleteByTrackedLinkId(Long id) {
        repository.deleteByTrackedLinkId(id);
    }

    @Override
    @Transactional
    public boolean saveIfAbsent(StackOverflowTrackingState stackOverflowTrackingState) {
        try {
            repository.saveAndFlush(toEntity(stackOverflowTrackingState));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public Optional<StackOverflowTrackingState> findByTrackedLink(TrackedLink trackedLink) {
        return repository.findById(trackedLink.getId()).map(entity -> toDomain(entity, trackedLink));
    }

    @Override
    public void clear() {
        entityManager
                .createNativeQuery("truncate table stackoverflow_tracking_state cascade")
                .executeUpdate();
    }

    private StackOverflowTrackingStateEntity toEntity(StackOverflowTrackingState state) {
        StackOverflowTrackingStateEntity entity = new StackOverflowTrackingStateEntity();
        entity.setTrackedLink(entityManager.getReference(
                TrackedLinkEntity.class, state.getTrackedLink().getId()));

        StackOverflowTimelineCursor cursor = state.getTimelineCursor();
        entity.setLastCreationDateEpochSec(cursor != null ? cursor.lastCreationDateEpochSec() : 0L);
        entity.setLastEventKey(cursor != null ? cursor.lastEventKey() : null);
        entity.setNextCheckAt(state.getNextCheckAt());
        entity.setLastQuestionActivityDateEpochSec(state.getLastQuestionActivityDateEpochSec());
        return entity;
    }

    private StackOverflowTrackingState toDomain(StackOverflowTrackingStateEntity entity, TrackedLink trackedLink) {
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setTimelineCursor(
                new StackOverflowTimelineCursor(entity.getLastCreationDateEpochSec(), entity.getLastEventKey()));
        state.setNextCheckAt(entity.getNextCheckAt());
        state.setLastQuestionActivityDateEpochSec(entity.getLastQuestionActivityDateEpochSec());
        return state;
    }
}
