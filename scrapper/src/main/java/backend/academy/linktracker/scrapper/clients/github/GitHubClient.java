package backend.academy.linktracker.scrapper.clients.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import java.io.IOException;
import backend.academy.linktracker.scrapper.exception.link.RepositoryPollingException;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class GitHubClient {
    private final RestClient gitHubRestClient;

    public GitHubRepositoryFetchResult fetchRepository(GitHubRepositoryKey key, String etag) {
        try {
            return gitHubRestClient.get()
                .uri("/repos/{owner}/{repo}", key.owner(), key.repo())
                .headers(headers -> {
                    if (StringUtils.hasText(etag)) {
                        headers.add(HttpHeaders.IF_NONE_MATCH, etag);
                    }
                })
                .exchange((request, response) -> new GitHubRepositoryFetchResult(
                    response.getStatusCode(),
                    response.getHeaders().getETag()
                ));
        } catch (RestClientException e) {
            throw new RepositoryPollingException(
                "Failed to call GitHub API for repository %s/%s".formatted(key.owner(), key.repo()),
                e
            );
        }
    }
}
