package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.ResourceKey;
import java.util.List;
import java.util.Optional;

public interface TrackedLinkRepository {
    Optional<TrackedLink> findByResourceKey(ResourceKey resourceKey);

    TrackedLink save(TrackedLink trackedLink);

    void delete(TrackedLink trackedLink);

    List<TrackedLink> findNextBatchAfterId(long lastSeenId, int limit);

    List<TrackedLink> findAll();

    void clear();
}
