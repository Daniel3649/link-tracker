package backend.academy.linktracker.scrapper.integration;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.domains.link.trackingstate.cursor.StackOverflowTimelineCursor;
import java.time.Instant;
import org.junit.jupiter.api.Test;

abstract class TrackingStateUpdateIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldUpdateGitHubTrackingState() {
        TrackedLink trackedLink = trackedLinkRepository.save(new TrackedLink(
                null, "https://github.com/octocat/Hello-World", new GitHubRepositoryKey("octocat", "Hello-World")));

        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setEtag("\"etag-1\"");
        state.setLastActivityId(1001L);

        assertThat(gitHubTrackingStateRepository.saveIfAbsent(state)).isTrue();

        state.setEtag("\"etag-2\"");
        state.setLastActivityId(2002L);
        gitHubTrackingStateRepository.save(state);

        GitHubTrackingState persisted =
                gitHubTrackingStateRepository.findByTrackedLink(trackedLink).orElseThrow();

        assertThat(persisted.getTrackedLink()).isEqualTo(trackedLink);
        assertThat(persisted.getEtag()).isEqualTo("\"etag-2\"");
        assertThat(persisted.getLastActivityId()).isEqualTo(2002L);
    }

    @Test
    void shouldUpdateStackOverflowTrackingState() {
        TrackedLink trackedLink = trackedLinkRepository.save(new TrackedLink(
                null,
                "https://stackoverflow.com/questions/12345678/example-question",
                new StackOverflowQuestionKey(12345678L)));

        StackOverflowTrackingState state = new StackOverflowTrackingState(trackedLink);
        state.setTimelineCursor(new StackOverflowTimelineCursor(1741680000L, "comment-1"));
        state.setNextCheckAt(Instant.parse("2026-03-22T10:15:30Z"));
        state.setLastQuestionActivityDateEpochSec(1741680000L);

        assertThat(stackOverflowTrackingStateRepository.saveIfAbsent(state)).isTrue();

        state.setTimelineCursor(new StackOverflowTimelineCursor(1741683600L, "answer-2"));
        state.setNextCheckAt(Instant.parse("2026-03-22T11:15:30Z"));
        state.setLastQuestionActivityDateEpochSec(1741683600L);
        stackOverflowTrackingStateRepository.save(state);

        StackOverflowTrackingState persisted = stackOverflowTrackingStateRepository
                .findByTrackedLink(trackedLink)
                .orElseThrow();

        assertThat(persisted.getTrackedLink()).isEqualTo(trackedLink);
        assertThat(persisted.getTimelineCursor()).isEqualTo(new StackOverflowTimelineCursor(1741683600L, "answer-2"));
        assertThat(persisted.getNextCheckAt()).isEqualTo(Instant.parse("2026-03-22T11:15:30Z"));
        assertThat(persisted.getLastQuestionActivityDateEpochSec()).isEqualTo(1741683600L);
    }
}
