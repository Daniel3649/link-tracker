package backend.academy.linktracker.scrapper.domains.link.trackingstate;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GitHubTrackingState {
    @EqualsAndHashCode.Include
    private final TrackedLink trackedLink;

    private Long lastActivityId;
}
