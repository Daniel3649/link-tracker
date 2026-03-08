package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(StackoverflowProperties.class)
public class StackOverflowClientConfig {

    @Bean
    public RestClient stackOverflowRestClient(
        RestClient.Builder builder,
        StackoverflowProperties properties
    ) {
        return builder
            .baseUrl(properties.getBaseUrl())
            .build();
    }
}
