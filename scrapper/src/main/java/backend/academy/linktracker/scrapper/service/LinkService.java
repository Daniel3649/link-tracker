package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

        try {
            MDC.put("url", uri.toString());
            MDC.put("resourceKey", resourceKey.toString());

            log.atDebug()
                    .addKeyValue("event", LogEvent.TRACKED_LINK_LOOKUP_STARTED)
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Tracked link lookup started");

            Optional<TrackedLink> trackedLink = trackedLinkRepository.findByResourceKey(resourceKey);

            log.atDebug()
                    .addKeyValue("event", "tracked_link_lookup_finished")
                    .addKeyValue("found", trackedLink.isPresent())
                    .addKeyValue(
                            "trackedLinkId", trackedLink.map(TrackedLink::getId).orElse(null))
                    .log("Tracked link lookup finished");

            return trackedLink;
        } finally {
            MDC.clear();
        }
    }

    public TrackedLink getOrCreateTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);

        ResourceKey resourceKey = parsedLink.resourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        try {
            MDC.put("url", parsedLink.url());
            MDC.put("resourceKey", resourceKey.toString());

            log.atInfo()
                    .addKeyValue("event", LogEvent.TRACKED_LINK_GET_OR_CREATE_STARTED)
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Tracked link get-or-create started");

            synchronized (lock) {
                return trackedLinkRepository
                        .findByResourceKey(resourceKey)
                        .map(trackedLink -> {
                            log.atInfo()
                                    .addKeyValue("event", LogEvent.TRACKED_LINK_REUSED)
                                    .addKeyValue("trackedLinkId", trackedLink.getId())
                                    .log("Tracked link reused");

                            return trackedLink;
                        })
                        .orElseGet(() -> {
                            TrackedLink createdTrackedLink = createTrackedLink(handler, parsedLink);

                            log.atInfo()
                                    .addKeyValue("event", LogEvent.TRACKED_LINK_CREATED)
                                    .addKeyValue("trackedLinkId", createdTrackedLink.getId())
                                    .log("Tracked link created");

                            return createdTrackedLink;
                        });
            }
        } finally {
            MDC.clear();
        }
    }

    public void deleteTrackedLinkWithState(TrackedLink trackedLink) {
        URI uri = URI.create(trackedLink.getUrl());
        LinkHandler handler = handlerRegistry.getHandler(uri);

        ResourceKey resourceKey = trackedLink.getResourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        try {
            MDC.put("url", uri.toString());
            MDC.put("resourceKey", resourceKey.toString());
            MDC.put("trackedLinkId", trackedLink.getId().toString());

            log.atInfo()
                    .addKeyValue("event", LogEvent.TRACKED_LINK_DELETE_STARTED)
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Tracked link deletion started");

            synchronized (lock) {
                handler.deleteTrackingState(trackedLink);
                trackedLinkRepository.deleteByResourceKey(resourceKey);

                log.atInfo().addKeyValue("event", LogEvent.TRACKED_LINK_DELETED).log("Tracked link deleted");
            }
        } finally {
            MDC.clear();
        }
    }

    private TrackedLink createTrackedLink(LinkHandler handler, ParsedLink parsedLink) {
        TrackedLink savedTrackedLink =
                trackedLinkRepository.save(new TrackedLink(null, parsedLink.url(), parsedLink.resourceKey()));

        try {
            MDC.put("trackedLinkId", savedTrackedLink.getId().toString());
            MDC.put("url", savedTrackedLink.getUrl());
            MDC.put("resourceKey", savedTrackedLink.getResourceKey().toString());

            log.atInfo().addKeyValue("event", LogEvent.TRACKED_LINK_SAVED).log("Tracked link saved");

            try {
                handler.createTrackingState(savedTrackedLink);

                log.atInfo()
                        .addKeyValue("event", LogEvent.TRACKING_STATE_CREATED)
                        .addKeyValue("handler", handler.getClass().getSimpleName())
                        .log("Tracking state created");

                return savedTrackedLink;
            } catch (RuntimeException e) {
                trackedLinkRepository.deleteByResourceKey(parsedLink.resourceKey());

                log.atWarn()
                        .setCause(e)
                        .addKeyValue("event", LogEvent.TRACKED_LINK_CREATION_FAILED)
                        .addKeyValue("handler", handler.getClass().getSimpleName())
                        .log("Tracked link creation failed, rollback applied");

                throw e;
            }
        } finally {
            MDC.clear();
        }
    }
}
