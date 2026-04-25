package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import java.util.List;
import java.util.Optional;

public interface TrackedLinkRepository {
    Optional<TrackedLink> findByResourceKey(ResourceKey resourceKey);

    TrackedLink saveIfAbsent(TrackedLink trackedLink);

    TrackedLink save(TrackedLink trackedLink);

    void deleteByResourceKey(ResourceKey resourceKey);

    List<TrackedLink> findAll();

    void clear();
}
