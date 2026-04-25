package backend.academy.linktracker.scrapper.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.common.LinkChange;
import backend.academy.linktracker.scrapper.common.LinkChangeDescriptionFormatter;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.schedule.LinkUpdateScheduler;
import backend.academy.linktracker.scrapper.sender.LinkUpdateSender;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateSchedulerTest {

    @Mock
    private TrackedLinkRepository trackedLinkRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private LinkHandlerRegistry linkHandlerRegistry;

    @Mock
    private LinkUpdateSender linkUpdateSender;

    @Mock
    private LinkHandler linkHandler;

    @Mock
    private SchedulerProperties schedulerProperties;

    private ExecutorService linkUpdateCheckExecutorService;

    private LinkChangeDescriptionFormatter linkChangeDescriptionFormatter;

    private LinkUpdateScheduler scheduler;

    @BeforeEach
    void setUp() {
        linkUpdateCheckExecutorService = Executors.newFixedThreadPool(4);
        linkChangeDescriptionFormatter = new LinkChangeDescriptionFormatter();
        scheduler = new LinkUpdateScheduler(
                trackedLinkRepository,
                subscriptionRepository,
                linkHandlerRegistry,
                linkUpdateSender,
                linkChangeDescriptionFormatter,
                schedulerProperties,
                linkUpdateCheckExecutorService);
    }

    @AfterEach
    void tearDownExecutor() {
        linkUpdateCheckExecutorService.shutdownNow();
    }

    @Test
    void shouldSendUpdateOnlyToUsersWhoTrackThisLink() {
        String url = "https://github.com/octocat/Hello-World";
        URI uri = URI.create(url);

        TrackedLink trackedLink = mock(TrackedLink.class);
        when(trackedLink.getId()).thenReturn(10L);
        when(trackedLink.getUrl()).thenReturn(url);

        LinkChange change = new LinkChange("New commit detected");

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(schedulerProperties.getLinkCheckParallelism()).thenReturn(1);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100)).thenReturn(List.of(trackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(uri)).thenReturn(linkHandler);
        when(linkHandler.checkForUpdate(trackedLink)).thenReturn(Optional.of(change));
        when(subscriptionRepository.findAllChatIdsByTrackedLinkId(10L)).thenReturn(List.of(101L, 202L));

        scheduler.checkUpdates();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(linkUpdateSender).send(captor.capture());

        LinkUpdate sentUpdate = captor.getValue();

        assertThat(sentUpdate.id()).isEqualTo(10L);
        assertThat(sentUpdate.url()).isEqualTo(uri);
        assertThat(sentUpdate.description()).isEqualTo("New commit detected");
        assertThat(sentUpdate.tgChatIds()).containsExactly(101L, 202L);
        assertThat(sentUpdate.tgChatIds()).doesNotContain(303L);
    }

    @Test
    void shouldNotSendUpdateWhenNobodyTracksLink() {
        String url = "https://github.com/octocat/Hello-World";
        URI uri = URI.create(url);

        TrackedLink trackedLink = mock(TrackedLink.class);
        when(trackedLink.getId()).thenReturn(10L);
        when(trackedLink.getUrl()).thenReturn(url);

        LinkChange change = new LinkChange("New commit detected");

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(schedulerProperties.getLinkCheckParallelism()).thenReturn(1);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100)).thenReturn(List.of(trackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(uri)).thenReturn(linkHandler);
        when(linkHandler.checkForUpdate(trackedLink)).thenReturn(Optional.of(change));
        when(subscriptionRepository.findAllChatIdsByTrackedLinkId(10L)).thenReturn(List.of());

        scheduler.checkUpdates();

        verify(linkUpdateSender, never()).send(any(LinkUpdate.class));
    }

    @Test
    void shouldProcessAllTrackedLinksAcrossMultipleBatches() {
        String firstUrl = "https://github.com/octocat/Hello-World";
        String secondUrl = "https://github.com/octocat/Spoon-Knife";

        TrackedLink firstTrackedLink = mock(TrackedLink.class);
        when(firstTrackedLink.getId()).thenReturn(10L);
        when(firstTrackedLink.getUrl()).thenReturn(firstUrl);

        TrackedLink secondTrackedLink = mock(TrackedLink.class);
        when(secondTrackedLink.getId()).thenReturn(20L);
        when(secondTrackedLink.getUrl()).thenReturn(secondUrl);

        LinkHandler firstHandler = mock(LinkHandler.class);
        LinkHandler secondHandler = mock(LinkHandler.class);

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(1);
        when(schedulerProperties.getLinkCheckParallelism()).thenReturn(1);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 1)).thenReturn(List.of(firstTrackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 1)).thenReturn(List.of(secondTrackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(20L, 1)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(URI.create(firstUrl))).thenReturn(firstHandler);
        when(linkHandlerRegistry.getHandler(URI.create(secondUrl))).thenReturn(secondHandler);
        when(firstHandler.checkForUpdate(firstTrackedLink)).thenReturn(Optional.empty());
        when(secondHandler.checkForUpdate(secondTrackedLink)).thenReturn(Optional.empty());

        scheduler.checkUpdates();

        verify(firstHandler).checkForUpdate(firstTrackedLink);
        verify(secondHandler).checkForUpdate(secondTrackedLink);
        verify(linkUpdateSender, never()).send(any(LinkUpdate.class));
    }

    @Test
    void shouldReturnFailuresReportForLinksThatCouldNotBeChecked() {
        String failedUrl = "https://github.com/octocat/Hello-World";
        String successfulUrl = "https://github.com/octocat/Spoon-Knife";

        TrackedLink failedTrackedLink = mock(TrackedLink.class);
        when(failedTrackedLink.getId()).thenReturn(10L);
        when(failedTrackedLink.getUrl()).thenReturn(failedUrl);

        TrackedLink successfulTrackedLink = mock(TrackedLink.class);
        when(successfulTrackedLink.getId()).thenReturn(20L);
        when(successfulTrackedLink.getUrl()).thenReturn(successfulUrl);

        LinkHandler failedHandler = mock(LinkHandler.class);
        LinkHandler successfulHandler = mock(LinkHandler.class);

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(schedulerProperties.getLinkCheckParallelism()).thenReturn(1);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100))
                .thenReturn(List.of(failedTrackedLink, successfulTrackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(20L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(URI.create(failedUrl))).thenReturn(failedHandler);
        when(linkHandlerRegistry.getHandler(URI.create(successfulUrl))).thenReturn(successfulHandler);
        when(failedHandler.checkForUpdate(failedTrackedLink))
                .thenThrow(new RepositoryPollingException("GitHub API unavailable"));
        when(successfulHandler.checkForUpdate(successfulTrackedLink)).thenReturn(Optional.empty());

        LinkUpdateScheduler.LinkUpdateCheckReport report = scheduler.runUpdateCheck();

        assertThat(report.processedLinksCount()).isEqualTo(2);
        assertThat(report.skipped()).isFalse();
        assertThat(report.failedLinks())
                .containsExactly(new LinkUpdateScheduler.LinkCheckFailure(
                        10L, failedUrl, RepositoryPollingException.class.getSimpleName(), "GitHub API unavailable"));
    }

    @Test
    void shouldProcessBatchInParallelWhenParallelismIsGreaterThanOne() throws Exception {
        String firstUrl = "https://github.com/octocat/Hello-World";
        String secondUrl = "https://github.com/octocat/Spoon-Knife";

        TrackedLink firstTrackedLink = mock(TrackedLink.class);
        when(firstTrackedLink.getId()).thenReturn(10L);
        when(firstTrackedLink.getUrl()).thenReturn(firstUrl);

        TrackedLink secondTrackedLink = mock(TrackedLink.class);
        when(secondTrackedLink.getId()).thenReturn(20L);
        when(secondTrackedLink.getUrl()).thenReturn(secondUrl);

        LinkHandler firstHandler = mock(LinkHandler.class);
        LinkHandler secondHandler = mock(LinkHandler.class);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(schedulerProperties.getLinkCheckParallelism()).thenReturn(2);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100))
                .thenReturn(List.of(firstTrackedLink, secondTrackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(20L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(URI.create(firstUrl))).thenReturn(firstHandler);
        when(linkHandlerRegistry.getHandler(URI.create(secondUrl))).thenReturn(secondHandler);
        when(firstHandler.checkForUpdate(firstTrackedLink)).thenAnswer(invocation -> {
            started.countDown();
            release.await(1, TimeUnit.SECONDS);
            return Optional.empty();
        });
        when(secondHandler.checkForUpdate(secondTrackedLink)).thenAnswer(invocation -> {
            started.countDown();
            release.await(1, TimeUnit.SECONDS);
            return Optional.empty();
        });

        Thread schedulerThread = new Thread(scheduler::checkUpdates);
        schedulerThread.start();

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        release.countDown();
        schedulerThread.join(1_000);

        assertThat(schedulerThread.isAlive()).isFalse();
    }

    @Test
    void shouldSkipConcurrentRunWhenPreviousRunIsStillInProgress() throws Exception {
        String url = "https://github.com/octocat/Hello-World";
        URI uri = URI.create(url);

        TrackedLink trackedLink = mock(TrackedLink.class);
        when(trackedLink.getId()).thenReturn(10L);
        when(trackedLink.getUrl()).thenReturn(url);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<LinkUpdateScheduler.LinkUpdateCheckReport> firstReport = new AtomicReference<>();

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(schedulerProperties.getLinkCheckParallelism()).thenReturn(1);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100)).thenReturn(List.of(trackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(uri)).thenReturn(linkHandler);
        when(linkHandler.checkForUpdate(trackedLink)).thenAnswer(invocation -> {
            started.countDown();
            release.await(1, TimeUnit.SECONDS);
            return Optional.empty();
        });

        Thread firstRunThread = new Thread(() -> firstReport.set(scheduler.runUpdateCheck()));
        firstRunThread.start();

        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        LinkUpdateScheduler.LinkUpdateCheckReport secondReport = scheduler.runUpdateCheck();
        assertThat(secondReport.skipped()).isTrue();
        assertThat(secondReport.processedLinksCount()).isZero();
        assertThat(secondReport.failedLinks()).isEmpty();

        release.countDown();
        firstRunThread.join(1_000);

        assertThat(firstRunThread.isAlive()).isFalse();
        assertThat(firstReport.get()).isNotNull();
        assertThat(firstReport.get().skipped()).isFalse();
        verify(linkHandler, times(1)).checkForUpdate(trackedLink);
    }

    @Test
    void shouldSendFailureReportToSubscribedChatsWhenPollingFails() {
        String failedUrl = "https://github.com/octocat/Hello-World";

        TrackedLink failedTrackedLink = mock(TrackedLink.class);
        when(failedTrackedLink.getId()).thenReturn(10L);
        when(failedTrackedLink.getUrl()).thenReturn(failedUrl);

        when(schedulerProperties.getLinkCheckBatchSize()).thenReturn(100);
        when(schedulerProperties.getLinkCheckParallelism()).thenReturn(1);
        when(trackedLinkRepository.findNextBatchAfterId(0L, 100)).thenReturn(List.of(failedTrackedLink));
        when(trackedLinkRepository.findNextBatchAfterId(10L, 100)).thenReturn(List.of());
        when(linkHandlerRegistry.getHandler(URI.create(failedUrl))).thenReturn(linkHandler);
        when(linkHandler.checkForUpdate(failedTrackedLink))
                .thenThrow(new RepositoryPollingException("GitHub API unavailable"));
        when(subscriptionRepository.findAllChatIdsByTrackedLinkId(10L)).thenReturn(List.of(101L));

        scheduler.checkUpdates();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(linkUpdateSender).send(captor.capture());

        assertThat(captor.getValue().id()).isEqualTo(10L);
        assertThat(captor.getValue().url()).isEqualTo(URI.create(failedUrl));
        assertThat(captor.getValue().tgChatIds()).containsExactly(101L);
        assertThat(captor.getValue().description())
                .contains("Link check report")
                .contains("GitHub API unavailable");
    }
}
