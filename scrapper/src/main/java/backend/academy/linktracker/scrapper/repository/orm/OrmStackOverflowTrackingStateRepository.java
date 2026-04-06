package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.cursor.StackOverflowTimelineCursor;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.StackOverflowTrackingStateEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.StackOverflowTrackingStateJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;

public class OrmStackOverflowTrackingStateRepository
        extends AbstractOrmTrackingStateRepository<StackOverflowTrackingState, StackOverflowTrackingStateEntity>
        implements StackOverflowTrackingStateRepository {
    private final StackOverflowTrackingStateJpaRepository repository;

    public OrmStackOverflowTrackingStateRepository(
            StackOverflowTrackingStateJpaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, "stackoverflow_tracking_state");
        this.repository = repository;
    }

    @Override
    public StackOverflowTrackingState save(StackOverflowTrackingState stackOverflowTrackingState) {
        return saveState(stackOverflowTrackingState, stackOverflowTrackingState.getTrackedLink());
    }

    @Override
    public void deleteByTrackedLinkId(Long id) {
        repository.deleteByTrackedLinkId(id);
    }

    @Override
    public boolean saveIfAbsent(StackOverflowTrackingState stackOverflowTrackingState) {
        return saveIfAbsentState(stackOverflowTrackingState);
    }

    @Override
    public Optional<StackOverflowTrackingState> findByTrackedLink(TrackedLink trackedLink) {
        return findStateByTrackedLink(trackedLink);
    }

    @Override
    protected StackOverflowTrackingStateEntity createNewEntity(StackOverflowTrackingState state) {
        StackOverflowTrackingStateEntity entity = new StackOverflowTrackingStateEntity();
        entity.setTrackedLink(trackedLinkReference(state.getTrackedLink().getId()));
        updateEntity(entity, state);
        return entity;
    }

    @Override
    protected StackOverflowTrackingStateEntity createEmptyEntity(Long trackedLinkId) {
        StackOverflowTrackingStateEntity entity = new StackOverflowTrackingStateEntity();
        entity.setTrackedLink(trackedLinkReference(trackedLinkId));
        return entity;
    }

    @Override
    protected void updateEntity(StackOverflowTrackingStateEntity entity, StackOverflowTrackingState state) {
        StackOverflowTimelineCursor cursor = state.getTimelineCursor();
        entity.setLastCreationDateEpochSec(cursor != null ? cursor.lastCreationDateEpochSec() : 0L);
        entity.setLastEventKey(cursor != null ? cursor.lastEventKey() : null);
        entity.setNextCheckAt(state.getNextCheckAt());
        entity.setLastQuestionActivityDateEpochSec(state.getLastQuestionActivityDateEpochSec());
    }

    @Override
    protected StackOverflowTrackingState toDomain(StackOverflowTrackingStateEntity entity, TrackedLink trackedLink) {
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setTimelineCursor(
                new StackOverflowTimelineCursor(entity.getLastCreationDateEpochSec(), entity.getLastEventKey()));
        state.setNextCheckAt(entity.getNextCheckAt());
        state.setLastQuestionActivityDateEpochSec(entity.getLastQuestionActivityDateEpochSec());
        return state;
    }
}
