package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.cursor.StackOverflowTimelineCursor;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
public class SqlStackOverflowTrackingStateRepository implements StackOverflowTrackingStateRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public StackOverflowTrackingState save(StackOverflowTrackingState stackOverflowTrackingState) {
        jdbcTemplate.update("""
                insert into stackoverflow_tracking_state (
                    link_id,
                    last_creation_date_epoch_sec,
                    last_event_key,
                    next_check_at,
                    last_question_activity_date_epoch_sec
                )
                values (
                    :linkId,
                    :lastCreationDateEpochSec,
                    :lastEventKey,
                    :nextCheckAt,
                    :lastQuestionActivityDateEpochSec
                )
                on conflict (link_id) do update
                set last_creation_date_epoch_sec = excluded.last_creation_date_epoch_sec,
                    last_event_key = excluded.last_event_key,
                    next_check_at = excluded.next_check_at,
                    last_question_activity_date_epoch_sec = excluded.last_question_activity_date_epoch_sec
                """, parameters(stackOverflowTrackingState));

        return stackOverflowTrackingState;
    }

    @Override
    public void deleteByTrackedLinkId(Long id) {
        jdbcTemplate.update(
                "delete from stackoverflow_tracking_state where link_id = :linkId",
                new MapSqlParameterSource("linkId", id));
    }

    @Override
    public boolean saveIfAbsent(StackOverflowTrackingState stackOverflowTrackingState) {
        return jdbcTemplate.update("""
                        insert into stackoverflow_tracking_state (
                            link_id,
                            last_creation_date_epoch_sec,
                            last_event_key,
                            next_check_at,
                            last_question_activity_date_epoch_sec
                        )
                        values (
                            :linkId,
                            :lastCreationDateEpochSec,
                            :lastEventKey,
                            :nextCheckAt,
                            :lastQuestionActivityDateEpochSec
                        )
                        on conflict (link_id) do nothing
                        """, parameters(stackOverflowTrackingState)) > 0;
    }

    @Override
    public Optional<StackOverflowTrackingState> findByTrackedLink(TrackedLink trackedLink) {
        return jdbcTemplate
                .query(
                        """
                        select
                            last_creation_date_epoch_sec,
                            last_event_key,
                            next_check_at,
                            last_question_activity_date_epoch_sec
                        from stackoverflow_tracking_state
                        where link_id = :linkId
                        """,
                        new MapSqlParameterSource("linkId", trackedLink.getId()),
                        (resultSet, rowNum) -> mapState(resultSet, trackedLink))
                .stream()
                .findFirst();
    }

    @Override
    public void clear() {
        jdbcTemplate.getJdbcTemplate().execute("truncate table stackoverflow_tracking_state cascade");
    }

    private MapSqlParameterSource parameters(StackOverflowTrackingState stackOverflowTrackingState) {
        StackOverflowTimelineCursor cursor = stackOverflowTrackingState.getTimelineCursor();
        Instant nextCheckAt = stackOverflowTrackingState.getNextCheckAt();

        return new MapSqlParameterSource()
                .addValue("linkId", stackOverflowTrackingState.getTrackedLink().getId())
                .addValue("lastCreationDateEpochSec", cursor != null ? cursor.lastCreationDateEpochSec() : 0L)
                .addValue("lastEventKey", cursor != null ? cursor.lastEventKey() : null)
                .addValue("nextCheckAt", nextCheckAt != null ? Timestamp.from(nextCheckAt) : null)
                .addValue(
                        "lastQuestionActivityDateEpochSec",
                        stackOverflowTrackingState.getLastQuestionActivityDateEpochSec());
    }

    private StackOverflowTrackingState mapState(java.sql.ResultSet resultSet, TrackedLink trackedLink)
            throws java.sql.SQLException {
        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);

        long lastCreationDateEpochSec = resultSet.getLong("last_creation_date_epoch_sec");
        String lastEventKey = resultSet.getString("last_event_key");
        state.setTimelineCursor(new StackOverflowTimelineCursor(lastCreationDateEpochSec, lastEventKey));

        Timestamp nextCheckAt = resultSet.getTimestamp("next_check_at");
        state.setNextCheckAt(nextCheckAt != null ? nextCheckAt.toInstant() : null);
        state.setLastQuestionActivityDateEpochSec(resultSet.getLong("last_question_activity_date_epoch_sec"));

        return state;
    }
}
