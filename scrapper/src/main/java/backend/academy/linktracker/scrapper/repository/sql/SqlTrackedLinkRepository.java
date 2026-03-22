package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
public class SqlTrackedLinkRepository implements TrackedLinkRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<TrackedLink> findByResourceKey(ResourceKey resourceKey) {
        return jdbcTemplate
                .query(
                        "select " + SqlTrackedLinkSupport.TRACKED_LINK_COLUMNS + " from tracked_link where "
                                + SqlTrackedLinkSupport.resourceKeyPredicate(resourceKey),
                        SqlTrackedLinkSupport.resourceKeyParams(resourceKey),
                        (resultSet, rowNum) -> SqlTrackedLinkSupport.mapTrackedLink(resultSet))
                .stream()
                .findFirst();
    }

    @Override
    public TrackedLink save(TrackedLink trackedLink) {
        MapSqlParameterSource parameters = SqlTrackedLinkSupport.trackedLinkParams(trackedLink);

        if (trackedLink.getId() != null) {
            jdbcTemplate.update(
                    """
                    update tracked_link
                    set url = :url,
                        link_type = :linkType,
                        github_owner = :githubOwner,
                        github_repo = :githubRepo,
                        stackoverflow_question_id = :stackoverflowQuestionId
                    where id = :id
                    """,
                    parameters);

            return trackedLink;
        }

        List<Long> insertedIds = jdbcTemplate.query(
                """
                insert into tracked_link (url, link_type, github_owner, github_repo, stackoverflow_question_id)
                values (:url, :linkType, :githubOwner, :githubRepo, :stackoverflowQuestionId)
                on conflict do nothing
                returning id
                """,
                parameters,
                (resultSet, rowNum) -> resultSet.getLong("id"));

        if (!insertedIds.isEmpty()) {
            return new TrackedLink(insertedIds.getFirst(), trackedLink.getUrl(), trackedLink.getResourceKey());
        }

        return findByResourceKey(trackedLink.getResourceKey()).orElseThrow();
    }

    @Override
    public void deleteByResourceKey(ResourceKey resourceKey) {
        jdbcTemplate.update(
                "delete from tracked_link where " + SqlTrackedLinkSupport.resourceKeyPredicate(resourceKey),
                SqlTrackedLinkSupport.resourceKeyParams(resourceKey));
    }

    @Override
    public List<TrackedLink> findNextBatchAfterId(long lastSeenId, int limit) {
        return jdbcTemplate.query(
                """
                select
                    %s
                from tracked_link
                where id > :lastSeenId
                order by id
                limit :limit
                """
                        .formatted(SqlTrackedLinkSupport.TRACKED_LINK_COLUMNS),
                new MapSqlParameterSource()
                        .addValue("lastSeenId", lastSeenId)
                        .addValue("limit", limit),
                (resultSet, rowNum) -> SqlTrackedLinkSupport.mapTrackedLink(resultSet));
    }

    @Override
    public List<TrackedLink> findAll() {
        return jdbcTemplate.query(
                "select " + SqlTrackedLinkSupport.TRACKED_LINK_COLUMNS + " from tracked_link order by id",
                (resultSet, rowNum) -> SqlTrackedLinkSupport.mapTrackedLink(resultSet));
    }

    @Override
    public void clear() {
        jdbcTemplate.getJdbcTemplate().execute("truncate table tracked_link cascade");
    }
}
