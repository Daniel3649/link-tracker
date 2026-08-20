package backend.academy.linktracker.scrapper.clients.github;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class GitHubClient {
    private static final ParameterizedTypeReference<List<GitHubRepositoryIssueResponse>> ISSUE_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient gitHubRestClient;

    public GitHubRepositoryFetchResult fetchRepository(GitHubRepositoryKey key) {
        try {
            return gitHubRestClient
                    .get()
                    .uri("/repos/{owner}/{repo}", key.owner(), key.repo())
                    .exchange((request, response) -> new GitHubRepositoryFetchResult(response.getStatusCode()));
        } catch (RestClientException e) {
            throw new RepositoryPollingException(
                    "Failed to call GitHub API for repository %s/%s".formatted(key.owner(), key.repo()), e);
        }
    }

    public List<GitHubRepositoryIssueResponse> fetchRecentIssuesAndPullRequests(GitHubRepositoryKey key, int perPage) {
        try {
            List<GitHubRepositoryIssueResponse> body = gitHubRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues")
                            .queryParam("state", "all")
                            .queryParam("sort", "created")
                            .queryParam("direction", "desc")
                            .queryParam("per_page", perPage)
                            .build(key.owner(), key.repo()))
                    .retrieve()
                    .body(ISSUE_LIST_TYPE);

            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            throw new RepositoryPollingException(
                    "Failed to fetch repository issues for %s/%s".formatted(key.owner(), key.repo()), e);
        }
    }
}
