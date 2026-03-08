package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(StackoverflowProperties.class)
public class StackOverflowClientConfig {

    @Bean
    public RestClient stackOverflowRestClient(RestClient.Builder builder, StackoverflowProperties properties) {
        return builder.baseUrl(properties.getBaseUrl()).build();
    }
}
