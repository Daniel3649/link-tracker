package backend.academy.linktracker.scrapper.models.link.trackingstate;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.cursor.GitHubCursor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GitHubTrackingState {
    @EqualsAndHashCode.Include
    private final TrackedLink trackedLink;

    private GitHubCursor cursor;
    private Instant nextCheckAt;
}
