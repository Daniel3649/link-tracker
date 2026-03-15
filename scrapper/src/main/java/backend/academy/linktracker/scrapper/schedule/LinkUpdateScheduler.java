package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.sender.LinkUpdateSender;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkUpdateScheduler {
    private final TrackedLinkRepository trackedLinkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LinkHandlerRegistry linkHandlerRegistry;
    private final LinkUpdateSender linkUpdateSender;

    @Scheduled(fixedDelayString = "${app.scheduler.link-check-delay-ms}")
    public void checkUpdates() {
        List<TrackedLink> trackedLinks = trackedLinkRepository.findAll();

        log.atInfo()
                .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_STARTED)
                .addKeyValue("trackedLinksCount", trackedLinks.size())
                .log("Link update check started");

        for (TrackedLink trackedLink : trackedLinks) {
            try {
                MDC.put("linkId", String.valueOf(trackedLink.getId()));
                MDC.put("url", trackedLink.getUrl());

                URI uri = URI.create(trackedLink.getUrl());
                LinkHandler handler = linkHandlerRegistry.getHandler(uri);

                handler.checkForUpdate(trackedLink).ifPresent(change -> sendUpdate(trackedLink, change));

                log.atInfo()
                        .addKeyValue("event", LogEvent.TRACKED_LINK_CHECK_FINISHED)
                        .log("Tracked link check finished");
            } catch (RepositoryPollingException e) {
                log.atWarn()
                        .setCause(e)
                        .addKeyValue("event", LogEvent.REPOSITORY_POLLING_FAILED)
                        .addKeyValue("exception", e.getClass().getSimpleName())
                        .log("Repository polling failed");
            } catch (Exception e) {
                log.atError()
                        .setCause(e)
                        .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_FAILED)
                        .addKeyValue("exception", e.getClass().getSimpleName())
                        .log("Unexpected error while checking link");
            }
        }
    }

    private void sendUpdate(TrackedLink trackedLink, LinkChange change) {
        List<Long> tgChatIds = subscriptionRepository.findAllByTrackedLink(trackedLink).stream()
                .map(subscription -> subscription.getTelegramChat().getId())
                .toList();
        try {
            MDC.put("linkId", String.valueOf(trackedLink.getId()));
            MDC.put("url", trackedLink.getUrl());

            if (tgChatIds.isEmpty()) {
                log.atWarn()
                        .addKeyValue("event", LogEvent.LINK_UPDATE_SKIPPED)
                        .addKeyValue("reason", "no_recipients")
                        .log("Link update skipped");

                return;
            }

            LinkUpdate update = new LinkUpdate(
                    trackedLink.getId(), URI.create(trackedLink.getUrl()), change.description(), tgChatIds);

            linkUpdateSender.send(update);

            log.atInfo()
                    .addKeyValue("event", LogEvent.LINK_UPDATE_NOTIFICATION)
                    .addKeyValue("recipientsCount", update.tgChatIds().size())
                    .log("Link update processed");
        } finally {
            MDC.clear();
        }
    }
}
