package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

abstract class AbstractOrmTrackingStateRepository<D, E> {
    private final JpaRepository<E, Long> repository;
    private final EntityManager entityManager;
    private final String tableName;

    protected AbstractOrmTrackingStateRepository(
            JpaRepository<E, Long> repository, EntityManager entityManager, String tableName) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.tableName = tableName;
    }

    @Transactional
    protected boolean saveIfAbsentState(D state) {
        try {
            repository.saveAndFlush(createNewEntity(state));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional
    protected D saveState(D state, TrackedLink trackedLink) {
        Long trackedLinkId = trackedLink.getId();
        E entity = repository.findById(trackedLinkId).orElseGet(() -> createEmptyEntity(trackedLinkId));
        updateEntity(entity, state);
        return toDomain(repository.saveAndFlush(entity), trackedLink);
    }

    protected Optional<D> findStateByTrackedLink(TrackedLink trackedLink) {
        return repository.findById(trackedLink.getId()).map(entity -> toDomain(entity, trackedLink));
    }

    public void clear() {
        entityManager
                .createNativeQuery("truncate table " + tableName + " cascade")
                .executeUpdate();
    }

    protected TrackedLinkEntity trackedLinkReference(Long trackedLinkId) {
        return entityManager.getReference(TrackedLinkEntity.class, trackedLinkId);
    }

    protected abstract E createNewEntity(D state);

    protected abstract E createEmptyEntity(Long trackedLinkId);

    protected abstract void updateEntity(E entity, D state);

    protected abstract D toDomain(E entity, TrackedLink trackedLink);
}
