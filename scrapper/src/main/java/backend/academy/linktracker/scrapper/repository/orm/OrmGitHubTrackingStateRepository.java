package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.GitHubTrackingStateEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.GitHubTrackingStateJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

@RequiredArgsConstructor
public class OrmGitHubTrackingStateRepository implements GitHubTrackingStateRepository {
    private final GitHubTrackingStateJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public boolean saveIfAbsent(GitHubTrackingState gitHubTrackingState) {
        try {
            repository.saveAndFlush(toEntity(gitHubTrackingState));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public GitHubTrackingState save(GitHubTrackingState gitHubTrackingState) {
        return toDomain(repository.saveAndFlush(toEntity(gitHubTrackingState)));
    }

    @Override
    public void deleteByTrackedLink(TrackedLink trackedLink) {
        repository.deleteById(trackedLink.getId());
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
        entity.setEtag(state.getEtag());
        entity.setLastActivityId(state.getLastActivityId());
        return entity;
    }

    private GitHubTrackingState toDomain(GitHubTrackingStateEntity entity) {
        return toDomain(entity, OrmTrackedLinkSupport.toDomain(entity.getTrackedLink()));
    }

    private GitHubTrackingState toDomain(GitHubTrackingStateEntity entity, TrackedLink trackedLink) {
        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setEtag(entity.getEtag());
        state.setLastActivityId(entity.getLastActivityId());
        return state;
    }
}
