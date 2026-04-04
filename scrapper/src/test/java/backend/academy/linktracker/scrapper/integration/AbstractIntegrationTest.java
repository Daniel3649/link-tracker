package backend.academy.linktracker.scrapper.integration;

import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ScrapperApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractIntegrationTest extends TestcontainersConfiguration {

    @Autowired
    protected TelegramChatRepository telegramChatRepository;

    @Autowired
    protected TrackedLinkRepository trackedLinkRepository;

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    @Autowired
    protected SubscriptionTagRepository subscriptionTagRepository;

    @Autowired
    protected GitHubTrackingStateRepository gitHubTrackingStateRepository;

    @Autowired
    protected StackOverflowTrackingStateRepository stackOverflowTrackingStateRepository;

    @BeforeEach
    void clearRepositories() {
        telegramChatRepository.clear();
        trackedLinkRepository.clear();
        subscriptionRepository.clear();
        subscriptionTagRepository.clear();
        gitHubTrackingStateRepository.clear();
        stackOverflowTrackingStateRepository.clear();
    }
}
