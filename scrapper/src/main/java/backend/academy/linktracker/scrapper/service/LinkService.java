package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkService {
    private final TrackedLinkRepository trackedLinkRepository;
    private final LinkHandlerRegistry handlerRegistry;

    private final ConcurrentMap<ResourceKey, Object> linkLocks = new ConcurrentHashMap<>();

    public Optional<TrackedLink> findTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);
        ResourceKey resourceKey = parsedLink.resourceKey();

        log.atDebug()
            .addKeyValue("event", "tracked_link_lookup_started")
            .addKeyValue("url", uri)
            .addKeyValue("resourceKey", resourceKey)
            .addKeyValue("handler", handler.getClass().getSimpleName())
            .log("Tracked link lookup started");

        Optional<TrackedLink> trackedLink = trackedLinkRepository.findByResourceKey(resourceKey);

        log.atDebug()
            .addKeyValue("event", "tracked_link_lookup_finished")
            .addKeyValue("url", uri)
            .addKeyValue("resourceKey", resourceKey)
            .addKeyValue("found", trackedLink.isPresent())
            .addKeyValue("trackedLinkId", trackedLink.map(TrackedLink::getId).orElse(null))
            .log("Tracked link lookup finished");

        return trackedLink;
    }

    public TrackedLink getOrCreateTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);

        ResourceKey resourceKey = parsedLink.resourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        log.atInfo()
            .addKeyValue("event", "tracked_link_get_or_create_started")
            .addKeyValue("url", parsedLink.url())
            .addKeyValue("resourceKey", resourceKey)
            .addKeyValue("handler", handler.getClass().getSimpleName())
            .log("Tracked link get-or-create started");

        synchronized (lock) {
            Optional<TrackedLink> existingTrackedLink = trackedLinkRepository.findByResourceKey(resourceKey);
            if (existingTrackedLink.isPresent()) {
                TrackedLink trackedLink = existingTrackedLink.get();

                log.atInfo()
                    .addKeyValue("event", "tracked_link_reused")
                    .addKeyValue("url", trackedLink.getUrl())
                    .addKeyValue("resourceKey", resourceKey)
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .log("Tracked link reused");

                return trackedLink;
            }

            TrackedLink createdTrackedLink = createTrackedLink(handler, parsedLink);

            log.atInfo()
                .addKeyValue("event", "tracked_link_created")
                .addKeyValue("url", createdTrackedLink.getUrl())
                .addKeyValue("resourceKey", resourceKey)
                .addKeyValue("trackedLinkId", createdTrackedLink.getId())
                .log("Tracked link created");

            return createdTrackedLink;
        }
    }

    public void deleteTrackedLinkWithState(TrackedLink trackedLink) {
        URI uri = URI.create(trackedLink.getUrl());
        LinkHandler handler = handlerRegistry.getHandler(uri);

        ResourceKey resourceKey = trackedLink.getResourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        log.atInfo()
            .addKeyValue("event", "tracked_link_delete_started")
            .addKeyValue("trackedLinkId", trackedLink.getId())
            .addKeyValue("url", trackedLink.getUrl())
            .addKeyValue("resourceKey", resourceKey)
            .addKeyValue("handler", handler.getClass().getSimpleName())
            .log("Tracked link deletion started");

        synchronized (lock) {
            handler.deleteTrackingState(trackedLink);
            trackedLinkRepository.deleteByResourceKey(resourceKey);

            log.atInfo()
                .addKeyValue("event", "tracked_link_deleted")
                .addKeyValue("trackedLinkId", trackedLink.getId())
                .addKeyValue("url", trackedLink.getUrl())
                .addKeyValue("resourceKey", resourceKey)
                .log("Tracked link deleted");
        }
    }

    private TrackedLink createTrackedLink(LinkHandler handler, ParsedLink parsedLink) {
        TrackedLink savedTrackedLink =
            trackedLinkRepository.save(new TrackedLink(null, parsedLink.url(), parsedLink.resourceKey()));

        log.atInfo()
            .addKeyValue("event", "tracked_link_saved")
            .addKeyValue("trackedLinkId", savedTrackedLink.getId())
            .addKeyValue("url", savedTrackedLink.getUrl())
            .addKeyValue("resourceKey", parsedLink.resourceKey())
            .log("Tracked link saved");

        try {
            handler.createTrackingState(savedTrackedLink);

            log.atInfo()
                .addKeyValue("event", "tracking_state_created")
                .addKeyValue("trackedLinkId", savedTrackedLink.getId())
                .addKeyValue("url", savedTrackedLink.getUrl())
                .addKeyValue("resourceKey", parsedLink.resourceKey())
                .addKeyValue("handler", handler.getClass().getSimpleName())
                .log("Tracking state created");

            return savedTrackedLink;
        } catch (RuntimeException e) {
            trackedLinkRepository.deleteByResourceKey(parsedLink.resourceKey());

            log.atWarn()
                .setCause(e)
                .addKeyValue("event", "tracked_link_creation_failed")
                .addKeyValue("trackedLinkId", savedTrackedLink.getId())
                .addKeyValue("url", savedTrackedLink.getUrl())
                .addKeyValue("resourceKey", parsedLink.resourceKey())
                .addKeyValue("handler", handler.getClass().getSimpleName())
                .log("Tracked link creation failed, rollback applied");

            throw e;
        }
    }
}
