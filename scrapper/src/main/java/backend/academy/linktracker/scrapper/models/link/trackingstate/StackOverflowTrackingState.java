package backend.academy.linktracker.scrapper.models.link.trackingstate;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.cursor.StackOverflowTimelineCursor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@RequiredArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StackOverflowTrackingState {
    @EqualsAndHashCode.Include
    private final TrackedLink trackedLink;

    private StackOverflowTimelineCursor timelineCursor;
    private Instant nextCheckAt;
}
