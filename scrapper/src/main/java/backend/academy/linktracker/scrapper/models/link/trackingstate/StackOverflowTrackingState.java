package backend.academy.linktracker.scrapper.models.link.trackingstate;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.trackingstate.cursor.StackOverflowTimelineCursor;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StackOverflowTrackingState {
    @EqualsAndHashCode.Include
    private final TrackedLink trackedLink;

    private StackOverflowTimelineCursor timelineCursor;
    private Instant nextCheckAt;
    private long lastQuestionActivityDateEpochSec;
}
