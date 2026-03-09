package backend.academy.linktracker.scrapper.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BotClientConfig {
    @Bean
    public RestClient botRestClient(
        RestClient.Builder builder,
        @Value("${app.bot.base-url}") String botBaseUrl
    ) {
        return builder
            .baseUrl(botBaseUrl)
            .build();
    }
}
