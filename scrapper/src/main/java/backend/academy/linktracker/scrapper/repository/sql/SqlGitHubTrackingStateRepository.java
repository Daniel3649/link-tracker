package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
public class SqlGitHubTrackingStateRepository implements GitHubTrackingStateRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean saveIfAbsent(GitHubTrackingState gitHubTrackingState) {
        return jdbcTemplate.update("""
                        insert into github_tracking_state (link_id, etag, last_activity_id)
                        values (:linkId, :etag, :lastActivityId)
                        on conflict (link_id) do nothing
                        """, parameters(gitHubTrackingState)) > 0;
    }

    @Override
    public GitHubTrackingState save(GitHubTrackingState gitHubTrackingState) {
        jdbcTemplate.update("""
                insert into github_tracking_state (link_id, etag, last_activity_id)
                values (:linkId, :etag, :lastActivityId)
                on conflict (link_id) do update
                set etag = excluded.etag,
                    last_activity_id = excluded.last_activity_id
                """, parameters(gitHubTrackingState));

        return gitHubTrackingState;
    }

    @Override
    public void deleteByTrackedLinkId(Long id) {
        jdbcTemplate.update(
                "delete from github_tracking_state where link_id = :linkId",
                new MapSqlParameterSource("linkId", id));
    }

    @Override
    public Optional<GitHubTrackingState> findByTrackedLink(TrackedLink trackedLink) {
        return jdbcTemplate
                .query(
                        """
                        select etag, last_activity_id
                        from github_tracking_state
                        where link_id = :linkId
                        """,
                        new MapSqlParameterSource("linkId", trackedLink.getId()),
                        (resultSet, rowNum) -> mapState(resultSet, trackedLink))
                .stream()
                .findFirst();
    }

    @Override
    public void clear() {
        jdbcTemplate.getJdbcTemplate().execute("truncate table github_tracking_state cascade");
    }

    private MapSqlParameterSource parameters(GitHubTrackingState gitHubTrackingState) {
        return new MapSqlParameterSource()
                .addValue("linkId", gitHubTrackingState.getTrackedLink().getId())
                .addValue("etag", gitHubTrackingState.getEtag())
                .addValue("lastActivityId", gitHubTrackingState.getLastActivityId());
    }

    private GitHubTrackingState mapState(java.sql.ResultSet resultSet, TrackedLink trackedLink)
            throws java.sql.SQLException {
        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setEtag(resultSet.getString("etag"));
        state.setLastActivityId((Long) resultSet.getObject("last_activity_id"));
        return state;
    }
}
