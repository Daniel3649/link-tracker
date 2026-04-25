package backend.academy.linktracker.scrapper.clients.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryActivityResponse;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class GitHubClient {
    private static final ParameterizedTypeReference<List<GitHubRepositoryActivityResponse>> ACTIVITY_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient gitHubRestClient;

    public GitHubRepositoryFetchResult fetchRepository(GitHubRepositoryKey key, String etag) {
        try {
            return gitHubRestClient
                    .get()
                    .uri("/repos/{owner}/{repo}", key.owner(), key.repo())
                    .headers(headers -> {
                        if (StringUtils.hasText(etag)) {
                            headers.add(HttpHeaders.IF_NONE_MATCH, etag);
                        }
                    })
                    .exchange((request, response) -> new GitHubRepositoryFetchResult(
                            response.getStatusCode(), response.getHeaders().getETag()));
        } catch (RestClientException e) {
            throw new RepositoryPollingException(
                    "Failed to call GitHub API for repository %s/%s".formatted(key.owner(), key.repo()), e);
        }
    }

    public List<GitHubRepositoryActivityResponse> fetchRecentActivities(GitHubRepositoryKey key, int perPage) {
        try {
            List<GitHubRepositoryActivityResponse> body = gitHubRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/activity")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", perPage)
                            .build(key.owner(), key.repo()))
                    .retrieve()
                    .body(ACTIVITY_LIST_TYPE);

            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            throw new RepositoryPollingException(
                    "Failed to fetch repository activity for %s/%s".formatted(key.owner(), key.repo()), e);
        }
    }
}
