package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.GitHubTrackingStateEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.GitHubTrackingStateJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class OrmGitHubTrackingStateRepository implements GitHubTrackingStateRepository {
    private final GitHubTrackingStateJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public boolean saveIfAbsent(GitHubTrackingState gitHubTrackingState) {
        try {
            repository.saveAndFlush(toEntity(gitHubTrackingState));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public GitHubTrackingState save(GitHubTrackingState gitHubTrackingState) {
        Long trackedLinkId = gitHubTrackingState.getTrackedLink().getId();
        GitHubTrackingStateEntity entity = repository.findById(trackedLinkId).orElseGet(() -> {
            GitHubTrackingStateEntity newEntity = new GitHubTrackingStateEntity();
            newEntity.setTrackedLink(entityManager.getReference(TrackedLinkEntity.class, trackedLinkId));
            return newEntity;
        });
        entity.setLastActivityId(gitHubTrackingState.getLastActivityId());
        return toDomain(repository.saveAndFlush(entity), gitHubTrackingState.getTrackedLink());
    }

    @Override
    public void deleteByTrackedLinkId(Long id) {
        repository.deleteByTrackedLinkId(id);
    }

    @Override
    public Optional<GitHubTrackingState> findByTrackedLink(TrackedLink trackedLink) {
        return repository.findById(trackedLink.getId()).map(entity -> toDomain(entity, trackedLink));
    }

    @Override
    public void clear() {
        entityManager
                .createNativeQuery("truncate table github_tracking_state cascade")
                .executeUpdate();
    }

    private GitHubTrackingStateEntity toEntity(GitHubTrackingState state) {
        GitHubTrackingStateEntity entity = new GitHubTrackingStateEntity();
        entity.setTrackedLink(entityManager.getReference(
                TrackedLinkEntity.class, state.getTrackedLink().getId()));
        entity.setLastActivityId(state.getLastActivityId());
        return entity;
    }

    private GitHubTrackingState toDomain(GitHubTrackingStateEntity entity, TrackedLink trackedLink) {
        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setLastActivityId(entity.getLastActivityId());
        return state;
    }
}
