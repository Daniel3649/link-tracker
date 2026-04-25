package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.LinkTypeEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackedLinkJpaRepository extends JpaRepository<TrackedLinkEntity, Long> {
    Optional<TrackedLinkEntity> findByLinkTypeAndGithubOwnerIgnoreCaseAndGithubRepoIgnoreCase(
            LinkTypeEntity linkType, String githubOwner, String githubRepo);

    Optional<TrackedLinkEntity> findByLinkTypeAndStackoverflowQuestionId(
            LinkTypeEntity linkType, Long stackoverflowQuestionId);

    @Query("""
            select tl
            from TrackedLinkEntity tl
            where tl.id > :lastSeenId
              and exists (
                  select 1
                  from SubscriptionEntity s
                  where s.linkId = tl.id
              )
            order by tl.id asc
            """)
    List<TrackedLinkEntity> findActiveByIdGreaterThanOrderByIdAsc(
            @Param("lastSeenId") Long lastSeenId, Pageable pageable);

    List<TrackedLinkEntity> findAllByOrderByIdAsc();

    long removeById(Long id);
}
