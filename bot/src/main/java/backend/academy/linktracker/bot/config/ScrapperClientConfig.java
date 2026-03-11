package backend.academy.linktracker.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ScrapperClientConfig {
    @Bean
    public RestClient scrapperRestClient(
        RestClient.Builder builder,
        @Value("${app.scrapper.base-url}") String baseUrl
    ) {
        return builder
            .baseUrl(baseUrl)
            .build();
    }
}
