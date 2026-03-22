package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.LinkTypeEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedLinkJpaRepository extends JpaRepository<TrackedLinkEntity, Long> {
    Optional<TrackedLinkEntity> findByLinkTypeAndGithubOwnerIgnoreCaseAndGithubRepoIgnoreCase(
            LinkTypeEntity linkType, String githubOwner, String githubRepo);

    Optional<TrackedLinkEntity> findByLinkTypeAndStackoverflowQuestionId(
            LinkTypeEntity linkType, Long stackoverflowQuestionId);

    List<TrackedLinkEntity> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    List<TrackedLinkEntity> findAllByOrderByIdAsc();
}
