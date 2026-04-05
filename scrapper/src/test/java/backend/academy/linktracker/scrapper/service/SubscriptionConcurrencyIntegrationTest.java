package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.scrapper.common.ParsedLink;
import backend.academy.linktracker.scrapper.common.PreparedTrackingState;
import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.integration.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.service.persistence.SubscriptionPersistenceService;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

abstract class SubscriptionConcurrencyIntegrationTest extends AbstractIntegrationTest {
    private static final URI URI = java.net.URI.create("https://github.com/octocat/Hello-World");
    private static final GitHubRepositoryKey RESOURCE_KEY = new GitHubRepositoryKey("octocat", "Hello-World");

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionPersistenceService subscriptionPersistenceService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private LinkHandlerRegistry linkHandlerRegistry;

    private ExecutorService executorService;
    private TransactionTemplate transactionTemplate;
    private LinkHandler handler;

    @BeforeEach
    void setUpConcurrencyTest() {
        executorService = Executors.newFixedThreadPool(2);
        transactionTemplate = new TransactionTemplate(transactionManager);
        handler = mock(LinkHandler.class);

        when(linkHandlerRegistry.getHandler(URI)).thenReturn(handler);
        when(handler.parse(URI)).thenReturn(new ParsedLink(URI.toString(), RESOURCE_KEY));
        when(handler.prepareTrackingState(any(ParsedLink.class))).thenReturn((PreparedTrackingState) trackedLink -> {});
    }

    @AfterEach
    void tearDownConcurrencyTest() {
        executorService.shutdownNow();
    }

    @Test
    void shouldKeepConcurrentAddedSubscriptionWhenRemovingLastExistingSubscription() throws Exception {
        telegramChatRepository.save(new TelegramChat(1L));
        telegramChatRepository.save(new TelegramChat(2L));

        AddLinkRequest addRequest = new AddLinkRequest(URI, Set.of(), List.of());
        RemoveLinkRequest removeRequest = new RemoveLinkRequest(URI);
        subscriptionService.addSubscription(1L, addRequest);

        TrackedLink trackedLink = trackedLinkRepository.findAll().getFirst();
        CountDownLatch trackedLinkLocked = new CountDownLatch(1);
        CountDownLatch allowConcurrentAdd = new CountDownLatch(1);

        Future<?> addFuture = executorService.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            TrackedLink lockedTrackedLink =
                    linkService.lockTrackedLink(trackedLink).orElseThrow();
            trackedLinkLocked.countDown();
            await(allowConcurrentAdd);
            subscriptionPersistenceService.createSubscription(lockedTrackedLink, new TelegramChat(2L), Set.of());
        }));

        assertThat(trackedLinkLocked.await(1, TimeUnit.SECONDS)).isTrue();

        Future<LinkResponse> removeFuture =
                executorService.submit(() -> subscriptionService.removeSubscription(1L, removeRequest));

        Thread.sleep(200);
        assertThat(removeFuture.isDone()).isFalse();

        allowConcurrentAdd.countDown();
        await(addFuture);
        LinkResponse removedSubscription = removeFuture.get(5, TimeUnit.SECONDS);

        assertThat(removedSubscription.url()).isEqualTo(URI);
        assertThat(subscriptionRepository.findAllByTelegramChatId(1L)).isEmpty();
        assertThat(subscriptionRepository.findAllByTelegramChatId(2L)).hasSize(1);
        assertThat(subscriptionRepository
                        .findAllByTelegramChatId(2L)
                        .getFirst()
                        .getTrackedLink()
                        .getUrl())
                .isEqualTo(URI.toString());
        assertThat(trackedLinkRepository.findAll()).hasSize(1);
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for latch", e);
        }
    }

    private void await(Future<?> future)
            throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        future.get(5, TimeUnit.SECONDS);
    }
}
