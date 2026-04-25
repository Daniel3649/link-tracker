package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTrackedLinkRepository implements TrackedLinkRepository {
    private final AtomicLong idSequence = new AtomicLong();
    private final ConcurrentMap<ResourceKey, TrackedLink> trackedLinks = new ConcurrentHashMap<>();

    @Override
    public Optional<TrackedLink> findByResourceKey(ResourceKey resourceKey) {
        return Optional.ofNullable(trackedLinks.get(resourceKey));
    }

    @Override
    public TrackedLink saveIfAbsent(TrackedLink trackedLink) {
        TrackedLink candidate = trackedLink.getId() != null
                ? trackedLink
                : new TrackedLink(idSequence.incrementAndGet(), trackedLink.getUrl(), trackedLink.getResourceKey());

        TrackedLink existing = trackedLinks.putIfAbsent(candidate.getResourceKey(), candidate);
        if (existing != null) {
            return existing;
        }

        return candidate;
    }

    @Override
    public TrackedLink save(TrackedLink trackedLink) {
        if (trackedLink.getId() != null) {
            trackedLinks.put(trackedLink.getResourceKey(), trackedLink);
            return trackedLink;
        }

        return saveIfAbsent(trackedLink);
    }

    @Override
    public void deleteByResourceKey(ResourceKey resourceKey) {
        trackedLinks.remove(resourceKey);
    }

    @Override
    public List<TrackedLink> findAll() {
        return trackedLinks.values().stream().toList();
    }

    @Override
    public void clear() {
        trackedLinks.clear();
    }
}
