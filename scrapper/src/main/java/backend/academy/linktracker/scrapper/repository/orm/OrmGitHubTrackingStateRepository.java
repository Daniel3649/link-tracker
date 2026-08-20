package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.GitHubTrackingStateEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.GitHubTrackingStateJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;

public class OrmGitHubTrackingStateRepository
        extends AbstractOrmTrackingStateRepository<GitHubTrackingState, GitHubTrackingStateEntity>
        implements GitHubTrackingStateRepository {
    private final GitHubTrackingStateJpaRepository repository;

    public OrmGitHubTrackingStateRepository(GitHubTrackingStateJpaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, "github_tracking_state");
        this.repository = repository;
    }

    @Override
    public boolean saveIfAbsent(GitHubTrackingState gitHubTrackingState) {
        return saveIfAbsentState(gitHubTrackingState);
    }

    @Override
    public GitHubTrackingState save(GitHubTrackingState gitHubTrackingState) {
        return saveState(gitHubTrackingState, gitHubTrackingState.getTrackedLink());
    }

    @Override
    public void deleteByTrackedLinkId(Long id) {
        repository.deleteByTrackedLinkId(id);
    }

    @Override
    public Optional<GitHubTrackingState> findByTrackedLink(TrackedLink trackedLink) {
        return findStateByTrackedLink(trackedLink);
    }

    @Override
    protected GitHubTrackingStateEntity createNewEntity(GitHubTrackingState state) {
        GitHubTrackingStateEntity entity = new GitHubTrackingStateEntity();
        entity.setTrackedLink(trackedLinkReference(state.getTrackedLink().getId()));
        updateEntity(entity, state);
        return entity;
    }

    @Override
    protected GitHubTrackingStateEntity createEmptyEntity(Long trackedLinkId) {
        GitHubTrackingStateEntity entity = new GitHubTrackingStateEntity();
        entity.setTrackedLink(trackedLinkReference(trackedLinkId));
        return entity;
    }

    @Override
    protected void updateEntity(GitHubTrackingStateEntity entity, GitHubTrackingState state) {
        entity.setLastActivityId(state.getLastActivityId());
    }

    @Override
    protected GitHubTrackingState toDomain(GitHubTrackingStateEntity entity, TrackedLink trackedLink) {
        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setLastActivityId(entity.getLastActivityId());
        return state;
    }
}
