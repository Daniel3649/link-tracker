package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.BotProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BotClientConfig {
    @Bean
    public RestClient botRestClient(RestClient.Builder builder, BotProperties botProperties) {
        return builder.baseUrl(botProperties.getBaseUrl()).build();
    }
}
