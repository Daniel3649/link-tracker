package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.contract.dto.request.TextNotification;
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
import backend.academy.linktracker.scrapper.sender.TextNotificationSender;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkUpdateScheduler {
    private static final String FAILURE_REPORT_HEADER = """
            Link check report
            The following tracked links could not be checked:""";
    private static final int FAILURE_REPORT_MESSAGE_LIMIT = 3500;

    private final AtomicBoolean updateCheckInProgress = new AtomicBoolean(false);

    private final TrackedLinkRepository trackedLinkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LinkHandlerRegistry linkHandlerRegistry;
    private final LinkUpdateSender linkUpdateSender;
    private final TextNotificationSender textNotificationSender;
    private final LinkChangeDescriptionFormatter linkChangeDescriptionFormatter;
    private final SchedulerProperties schedulerProperties;

    @Qualifier("linkUpdateCheckExecutorService")
    private final ExecutorService linkUpdateCheckExecutorService;

    @Scheduled(fixedDelayString = "${app.scheduler.link-check-delay-ms}")
    public void checkUpdates() {
        int batchSize = schedulerProperties.getLinkCheckBatchSize();
        int parallelism = schedulerProperties.getLinkCheckParallelism();
        log.atInfo()
                .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_STARTED)
                .addKeyValue("batchSize", batchSize)
                .addKeyValue("parallelism", parallelism)
                .log("Link update check started");

        LinkUpdateCheckReport report = runUpdateCheck();

        log.atInfo()
                .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_FINISHED)
                .addKeyValue("processedLinksCount", report.processedLinksCount())
                .addKeyValue("failedLinksCount", report.failedLinks().size())
                .addKeyValue("skipped", report.skipped())
                .log("Link update check finished");

        if (!report.skipped() && !report.failedLinks().isEmpty()) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_REPORT)
                    .addKeyValue("failedLinksCount", report.failedLinks().size())
                    .addKeyValue("failedLinks", report.failedLinks())
                    .log("Link update check failures report");

            sendFailureReports(report.failedLinks());
        }
    }

    public LinkUpdateCheckReport runUpdateCheck() {
        if (!updateCheckInProgress.compareAndSet(false, true)) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_SKIPPED)
                    .log("Link update check skipped because previous run is still in progress");
            return new LinkUpdateCheckReport(0, List.of(), true);
        }

        int batchSize = schedulerProperties.getLinkCheckBatchSize();
        int parallelism = schedulerProperties.getLinkCheckParallelism();
        long lastSeenId = 0L;
        int processedLinksCount = 0;
        List<LinkCheckFailure> failures = new ArrayList<>();

        try {
            while (true) {
                List<TrackedLink> trackedLinks = trackedLinkRepository.findNextBatchAfterId(lastSeenId, batchSize);
                if (trackedLinks.isEmpty()) {
                    break;
                }

                failures.addAll(processBatch(trackedLinks, parallelism));
                processedLinksCount += trackedLinks.size();
                lastSeenId = trackedLinks.getLast().getId();
            }
        } finally {
            updateCheckInProgress.set(false);
        }

        return new LinkUpdateCheckReport(processedLinksCount, List.copyOf(failures), false);
    }

    private List<LinkCheckFailure> processBatch(List<TrackedLink> trackedLinks, int parallelism) {
        if (parallelism <= 1 || trackedLinks.size() <= 1) {
            return trackedLinks.stream()
                    .map(this::checkTrackedLink)
                    .flatMap(Optional::stream)
                    .toList();
        }

        List<Future<Optional<LinkCheckFailure>>> futures = trackedLinks.stream()
                .map(trackedLink -> linkUpdateCheckExecutorService.submit(() -> checkTrackedLink(trackedLink)))
                .toList();

        List<LinkCheckFailure> failures = new ArrayList<>();
        for (int index = 0; index < trackedLinks.size(); index++) {
            TrackedLink trackedLink = trackedLinks.get(index);
            try {
                futures.get(index).get().ifPresent(failures::add);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failures.add(buildFailure(trackedLink, e));
                break;
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                failures.add(buildFailure(trackedLink, cause));
            }
        }

        return failures;
    }

    private Optional<LinkCheckFailure> checkTrackedLink(TrackedLink trackedLink) {
        try (var _ = MDC.putCloseable("linkId", String.valueOf(trackedLink.getId()));
                var _ = MDC.putCloseable("url", trackedLink.getUrl())) {
            URI uri = URI.create(trackedLink.getUrl());
            LinkHandler handler = linkHandlerRegistry.getHandler(uri);

            handler.checkForUpdate(trackedLink).ifPresent(change -> sendUpdate(trackedLink, change));

            log.atInfo()
                    .addKeyValue("event", LogEvent.TRACKED_LINK_CHECK_FINISHED)
                    .log("Tracked link check finished");

            return Optional.empty();
        } catch (RepositoryPollingException e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("event", LogEvent.REPOSITORY_POLLING_FAILED)
                    .addKeyValue("exception", e.getClass().getSimpleName())
                    .log("Repository polling failed");
            return Optional.of(buildFailure(trackedLink, e));
        } catch (Exception e) {
            log.atError()
                    .setCause(e)
                    .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_FAILED)
                    .addKeyValue("exception", e.getClass().getSimpleName())
                    .log("Unexpected error while checking link");
            return Optional.of(buildFailure(trackedLink, e));
        }
    }

    private LinkCheckFailure buildFailure(TrackedLink trackedLink, Throwable throwable) {
        return new LinkCheckFailure(
                trackedLink.getId(),
                trackedLink.getUrl(),
                throwable.getClass().getSimpleName(),
                throwable.getMessage());
    }

    private void sendUpdate(TrackedLink trackedLink, LinkChange change) {
        List<Long> tgChatIds = subscriptionRepository.findAllChatIdsByTrackedLinkId(trackedLink.getId()).stream()
                .distinct()
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

    private void sendFailureReports(List<LinkCheckFailure> failedLinks) {
        Map<Long, List<LinkCheckFailure>> failuresByChatId = groupFailuresByChatId(failedLinks);

        for (Map.Entry<Long, List<LinkCheckFailure>> entry : failuresByChatId.entrySet()) {
            long chatId = entry.getKey();
            List<String> reports = buildFailureReports(entry.getValue());

            for (String report : reports) {
                try {
                    textNotificationSender.send(new TextNotification(report, List.of(chatId)));
                } catch (Exception e) {
                    log.atWarn()
                            .setCause(e)
                            .addKeyValue("event", LogEvent.LINK_UPDATE_CHECK_REPORT)
                            .addKeyValue("chatId", chatId)
                            .addKeyValue("failedLinksCount", entry.getValue().size())
                            .log("Failed to send link check failure report");
                }
            }
        }
    }

    private Map<Long, List<LinkCheckFailure>> groupFailuresByChatId(List<LinkCheckFailure> failedLinks) {
        Map<Long, List<LinkCheckFailure>> failuresByChatId = new LinkedHashMap<>();

        for (LinkCheckFailure failure : failedLinks) {
            List<Long> chatIds = subscriptionRepository.findAllChatIdsByTrackedLinkId(failure.linkId()).stream()
                    .distinct()
                    .toList();

            for (Long chatId : chatIds) {
                failuresByChatId
                        .computeIfAbsent(chatId, ignored -> new ArrayList<>())
                        .add(failure);
            }
        }

        return failuresByChatId;
    }

    private List<String> buildFailureReports(List<LinkCheckFailure> failedLinks) {
        List<String> lines =
                failedLinks.stream().map(this::formatFailureLine).distinct().toList();
        List<String> reports = new ArrayList<>();
        StringBuilder currentReport = new StringBuilder(FAILURE_REPORT_HEADER);

        for (String line : lines) {
            if (currentReport.length() + 1 + line.length() > FAILURE_REPORT_MESSAGE_LIMIT
                    && currentReport.length() > FAILURE_REPORT_HEADER.length()) {
                reports.add(currentReport.toString());
                currentReport = new StringBuilder(FAILURE_REPORT_HEADER);
            }

            currentReport.append('\n').append(line);
        }

        reports.add(currentReport.toString());
        return reports;
    }

    private String formatFailureLine(LinkCheckFailure failure) {
        StringBuilder builder = new StringBuilder("- ").append(failure.url());

        if (failure.exception() != null && !failure.exception().isBlank()) {
            builder.append(" (").append(failure.exception());

            if (failure.message() != null && !failure.message().isBlank()) {
                builder.append(": ").append(normalizeInline(failure.message()));
            }

            builder.append(')');
        }

        return builder.toString();
    }

    private String normalizeInline(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    public record LinkUpdateCheckReport(int processedLinksCount, List<LinkCheckFailure> failedLinks, boolean skipped) {}

    public record LinkCheckFailure(Long linkId, String url, String exception, String message) {}
}
