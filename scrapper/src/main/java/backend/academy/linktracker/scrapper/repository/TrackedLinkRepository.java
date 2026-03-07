package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import java.util.Optional;

public interface TrackedLinkRepository {
    Optional<TrackedLink> findByResourceKey(ResourceKey resourceKey);
    TrackedLink save(TrackedLink trackedLink);


}
