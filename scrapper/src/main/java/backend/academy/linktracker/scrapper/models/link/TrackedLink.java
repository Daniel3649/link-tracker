package backend.academy.linktracker.scrapper.models.link;

import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@Getter
public class TrackedLink {
    private final Long id;

    private final String url;

    @EqualsAndHashCode.Include
    private final ResourceKey resourceKey;
}
