package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.GithubProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(GithubProperties.class)
public class GitHubClientConfig {

    @Bean
    public RestClient gitHubRestClient(
        RestClient.Builder builder,
        GithubProperties properties
    ) {
        return builder
            .baseUrl(properties.getBaseUrl())
            .defaultHeaders(headers -> {
                headers.add(HttpHeaders.ACCEPT, "application/vnd.github+json");
                headers.add("X-GitHub-Api-Version", "2022-11-28");

                if (StringUtils.hasText(properties.getToken())) {
                    headers.setBearerAuth(properties.getToken());
                }
            })
            .build();
    }


}
