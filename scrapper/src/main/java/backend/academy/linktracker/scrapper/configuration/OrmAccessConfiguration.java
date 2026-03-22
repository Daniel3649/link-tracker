package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmGitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmStackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmSubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmTelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmTrackedLinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.GitHubTrackingStateJpaRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.StackOverflowTrackingStateJpaRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.SubscriptionJpaRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.SubscriptionTagJpaRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TelegramChatJpaRepository;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TrackedLinkJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.database", name = "access-type", havingValue = "ORM")
public class OrmAccessConfiguration {
    @Bean
    TelegramChatRepository telegramChatRepository(TelegramChatJpaRepository repository, EntityManager entityManager) {
        return new OrmTelegramChatRepository(repository, entityManager);
    }

    @Bean
    TrackedLinkRepository trackedLinkRepository(TrackedLinkJpaRepository repository, EntityManager entityManager) {
        return new OrmTrackedLinkRepository(repository, entityManager);
    }

    @Bean
    SubscriptionRepository subscriptionRepository(SubscriptionJpaRepository repository, EntityManager entityManager) {
        return new OrmSubscriptionRepository(repository, entityManager);
    }

    @Bean
    SubscriptionTagRepository subscriptionTagRepository(
            SubscriptionTagJpaRepository repository, EntityManager entityManager) {
        return new OrmSubscriptionTagRepository(repository, entityManager);
    }

    @Bean
    GitHubTrackingStateRepository gitHubTrackingStateRepository(
            GitHubTrackingStateJpaRepository repository, EntityManager entityManager) {
        return new OrmGitHubTrackingStateRepository(repository, entityManager);
    }

    @Bean
    StackOverflowTrackingStateRepository stackOverflowTrackingStateRepository(
            StackOverflowTrackingStateJpaRepository repository, EntityManager entityManager) {
        return new OrmStackOverflowTrackingStateRepository(repository, entityManager);
    }
}
