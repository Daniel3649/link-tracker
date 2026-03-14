package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.properties.ScrapperProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ScrapperClientConfig {
    @Bean
    public RestClient scrapperRestClient(
        RestClient.Builder builder,
        ScrapperProperties properties
    ) {
        return builder
            .baseUrl(properties.getBaseUrl())
            .build();
    }
}
