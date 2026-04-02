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
        return trackedLinkRepository.findByResourceKey(parsedLink.resourceKey());
    }

    public TrackedLink getOrCreateTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);

        ResourceKey resourceKey = parsedLink.resourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        try (var urlMdc = MDC.putCloseable("url", parsedLink.url());
            var resourceKeyMdc = MDC.putCloseable("resourceKey", resourceKey.toString())) {
            synchronized (lock) {
                return trackedLinkRepository
                        .findByResourceKey(resourceKey)
                        .map(trackedLink -> {
                            log.atDebug()
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
                                    .addKeyValue("handler", handler.getClass().getSimpleName())
                                    .log("Tracked link created");

                            return createdTrackedLink;
                        });
            }
        }
    }

    public void deleteTrackedLinkWithState(TrackedLink trackedLink) {
        URI uri = URI.create(trackedLink.getUrl());
        LinkHandler handler = handlerRegistry.getHandler(uri);

        ResourceKey resourceKey = trackedLink.getResourceKey();
        Object lock = linkLocks.computeIfAbsent(resourceKey, ignored -> new Object());

        try (var urlMdc = MDC.putCloseable("url", uri.toString());
            var resourceKeyMdc = MDC.putCloseable("resourceKey", resourceKey.toString());
            var trackedLinkIdMdc = MDC.putCloseable("trackedLinkId", trackedLink.getId().toString())) {
            synchronized (lock) {
                handler.deleteTrackingState(trackedLink);
                trackedLinkRepository.deleteByResourceKey(resourceKey);

                log.atInfo()
                        .addKeyValue("event", LogEvent.TRACKED_LINK_DELETED)
                        .addKeyValue("handler", handler.getClass().getSimpleName())
                        .log("Tracked link deleted");
            }
        }
    }

    private TrackedLink createTrackedLink(LinkHandler handler, ParsedLink parsedLink) {
        TrackedLink savedTrackedLink =
                trackedLinkRepository.save(new TrackedLink(null, parsedLink.url(), parsedLink.resourceKey()));

        try (var trackedLinkIdMdc = MDC.putCloseable("trackedLinkId", savedTrackedLink.getId().toString())) {
            try {
                handler.createTrackingState(savedTrackedLink);
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
        }
    }
}
