package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlGitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlStackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlSubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlSubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlTelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.sql.SqlTrackedLinkRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.database", name = "access-type", havingValue = "SQL")
public class SqlAccessConfiguration {
    @Bean
    TelegramChatRepository telegramChatRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlTelegramChatRepository(jdbcTemplate);
    }

    @Bean
    TrackedLinkRepository trackedLinkRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlTrackedLinkRepository(jdbcTemplate);
    }

    @Bean
    SubscriptionRepository subscriptionRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlSubscriptionRepository(jdbcTemplate);
    }

    @Bean
    SubscriptionTagRepository subscriptionTagRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlSubscriptionTagRepository(jdbcTemplate);
    }

    @Bean
    GitHubTrackingStateRepository gitHubTrackingStateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlGitHubTrackingStateRepository(jdbcTemplate);
    }

    @Bean
    StackOverflowTrackingStateRepository stackOverflowTrackingStateRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlStackOverflowTrackingStateRepository(jdbcTemplate);
    }
}
