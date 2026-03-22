package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.LinkTypeEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TrackedLinkJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

@RequiredArgsConstructor
public class OrmTrackedLinkRepository implements TrackedLinkRepository {
    private final TrackedLinkJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public Optional<TrackedLink> findByResourceKey(ResourceKey resourceKey) {
        return findEntityByResourceKey(resourceKey).map(OrmTrackedLinkSupport::toDomain);
    }

    @Override
    public TrackedLink save(TrackedLink trackedLink) {
        if (trackedLink.getId() != null) {
            TrackedLinkEntity entity = repository.findById(trackedLink.getId()).orElseThrow();
            OrmTrackedLinkSupport.fillEntity(entity, trackedLink);
            return OrmTrackedLinkSupport.toDomain(repository.saveAndFlush(entity));
        }

        Optional<TrackedLinkEntity> existing = findEntityByResourceKey(trackedLink.getResourceKey());
        if (existing.isPresent()) {
            return OrmTrackedLinkSupport.toDomain(existing.get());
        }

        TrackedLinkEntity saved = repository.saveAndFlush(OrmTrackedLinkSupport.toNewEntity(trackedLink));
        return OrmTrackedLinkSupport.toDomain(saved);
    }

    @Override
    public void deleteByResourceKey(ResourceKey resourceKey) {
        findEntityByResourceKey(resourceKey).ifPresent(repository::delete);
    }

    @Override
    public List<TrackedLink> findNextBatchAfterId(long lastSeenId, int limit) {
        return repository.findByIdGreaterThanOrderByIdAsc(lastSeenId, PageRequest.of(0, limit)).stream()
                .map(OrmTrackedLinkSupport::toDomain)
                .toList();
    }

    @Override
    public List<TrackedLink> findAll() {
        return repository.findAllByOrderByIdAsc().stream().map(OrmTrackedLinkSupport::toDomain).toList();
    }

    @Override
    public void clear() {
        entityManager.createNativeQuery("truncate table tracked_link cascade").executeUpdate();
    }

    private Optional<TrackedLinkEntity> findEntityByResourceKey(ResourceKey resourceKey) {
        return switch (resourceKey) {
            case GitHubRepositoryKey gitHubRepositoryKey ->
                repository.findByLinkTypeAndGithubOwnerIgnoreCaseAndGithubRepoIgnoreCase(
                        LinkTypeEntity.GITHUB, gitHubRepositoryKey.owner(), gitHubRepositoryKey.repo());
            case StackOverflowQuestionKey stackOverflowQuestionKey ->
                repository.findByLinkTypeAndStackoverflowQuestionId(
                        LinkTypeEntity.STACKOVERFLOW, stackOverflowQuestionKey.questionId());
        };
    }
}
