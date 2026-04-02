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

    public Optional<TrackedLink> findTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);
        return trackedLinkRepository.findByResourceKey(parsedLink.resourceKey());
    }

    public TrackedLink getOrCreateTrackedLink(URI uri) {
        LinkHandler handler = handlerRegistry.getHandler(uri);
        ParsedLink parsedLink = handler.parse(uri);

        ResourceKey resourceKey = parsedLink.resourceKey();

        try (var urlMdc = MDC.putCloseable("url", parsedLink.url());
            var resourceKeyMdc = MDC.putCloseable("resourceKey", resourceKey.toString())) {
            Optional<TrackedLink> existingTrackedLink = trackedLinkRepository.findByResourceKey(resourceKey);
            if (existingTrackedLink.isPresent()) {
                TrackedLink trackedLink = existingTrackedLink.orElseThrow();

                log.atDebug()
                        .addKeyValue("event", LogEvent.TRACKED_LINK_RESOLVED)
                        .addKeyValue("trackedLinkId", trackedLink.getId())
                        .log("Tracked link resolved");

                return trackedLink;
            }

            TrackedLink trackedLinkCandidate = new TrackedLink(null, parsedLink.url(), parsedLink.resourceKey());
            initializeTrackingState(handler, trackedLinkCandidate);

            TrackedLink trackedLink = trackedLinkRepository.saveIfAbsent(trackedLinkCandidate);

            log.atDebug()
                    .addKeyValue("event", LogEvent.TRACKED_LINK_RESOLVED)
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Tracked link resolved");

            return trackedLink;
        }
    }

    public void deleteTrackedLinkWithState(TrackedLink trackedLink) {
        URI uri = URI.create(trackedLink.getUrl());
        LinkHandler handler = handlerRegistry.getHandler(uri);

        ResourceKey resourceKey = trackedLink.getResourceKey();

        try (var urlMdc = MDC.putCloseable("url", uri.toString());
            var resourceKeyMdc = MDC.putCloseable("resourceKey", resourceKey.toString());
            var trackedLinkIdMdc = MDC.putCloseable("trackedLinkId", trackedLink.getId().toString())) {
            handler.deleteTrackingState(trackedLink);
            trackedLinkRepository.deleteByResourceKey(resourceKey);

            log.atInfo()
                    .addKeyValue("event", LogEvent.TRACKED_LINK_DELETED)
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Tracked link deleted");
        }
    }

    private void initializeTrackingState(LinkHandler handler, TrackedLink trackedLink) {
        try {
            handler.createTrackingState(trackedLink);
        } catch (RuntimeException e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("event", LogEvent.TRACKED_LINK_CREATION_FAILED)
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Tracked link creation failed");

            throw e;
        }
    }
}
