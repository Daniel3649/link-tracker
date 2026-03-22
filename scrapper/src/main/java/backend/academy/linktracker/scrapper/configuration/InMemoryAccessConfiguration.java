package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryGithubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryStackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemorySubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemorySubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryTelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.impl.InMemoryTrackedLinkRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("test")
@ConditionalOnMissingBean({NamedParameterJdbcTemplate.class, EntityManagerFactory.class})
public class InMemoryAccessConfiguration {
    @Bean
    TelegramChatRepository telegramChatRepository() {
        return new InMemoryTelegramChatRepository();
    }

    @Bean
    TrackedLinkRepository trackedLinkRepository() {
        return new InMemoryTrackedLinkRepository();
    }

    @Bean
    SubscriptionRepository subscriptionRepository() {
        return new InMemorySubscriptionRepository();
    }

    @Bean
    SubscriptionTagRepository subscriptionTagRepository() {
        return new InMemorySubscriptionTagRepository();
    }

    @Bean
    GitHubTrackingStateRepository gitHubTrackingStateRepository() {
        return new InMemoryGithubTrackingStateRepository();
    }

    @Bean
    StackOverflowTrackingStateRepository stackOverflowTrackingStateRepository() {
        return new InMemoryStackOverflowTrackingStateRepository();
    }
}
