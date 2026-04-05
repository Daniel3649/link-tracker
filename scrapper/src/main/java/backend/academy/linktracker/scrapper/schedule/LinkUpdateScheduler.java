package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangeDescriptionFormatter;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
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
    private final LinkChangeDescriptionFormatter linkChangeDescriptionFormatter;
    private final SchedulerProperties schedulerProperties;

    @Scheduled(fixedDelayString = "${app.scheduler.link-check-delay-ms}")
    public void checkUpdates() {
        int batchSize = schedulerProperties.getLinkCheckBatchSize();
        long lastSeenId = 0L;
        int processedLinksCount = 0;
        log.atInfo()
                .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_STARTED)
                .addKeyValue("batchSize", batchSize)
                .log("Link update check started");

        while (true) {
            List<TrackedLink> trackedLinks = trackedLinkRepository.findNextBatchAfterId(lastSeenId, batchSize);
            if (trackedLinks.isEmpty()) {
                break;
            }

            for (TrackedLink trackedLink : trackedLinks) {
                try (var _ = MDC.putCloseable("linkId", String.valueOf(trackedLink.getId()));
                        var _ = MDC.putCloseable("url", trackedLink.getUrl())) {
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

            processedLinksCount += trackedLinks.size();
            lastSeenId = trackedLinks.getLast().getId();
        }

        log.atInfo()
                .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_FINISHED)
                .addKeyValue("processedLinksCount", processedLinksCount)
                .log("Link update check finished");
    }

    private void sendUpdate(TrackedLink trackedLink, LinkChange change) {
        List<Long> tgChatIds = subscriptionRepository.findAllByTrackedLink(trackedLink).stream()
                .map(subscription -> subscription.getTelegramChat().id())
                .toList();
        if (tgChatIds.isEmpty()) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.LINK_UPDATE_SKIPPED)
                    .addKeyValue("reason", "no_recipients")
                    .log("Link update skipped");

            return;
        }

        LinkUpdate update = new LinkUpdate(
                trackedLink.getId(),
                URI.create(trackedLink.getUrl()),
                linkChangeDescriptionFormatter.format(change),
                tgChatIds);

        linkUpdateSender.send(update);

        log.atInfo()
                .addKeyValue("event", LogEvent.LINK_UPDATE_NOTIFICATION)
                .addKeyValue("recipientsCount", update.tgChatIds().size())
                .log("Link update processed");
    }
}
