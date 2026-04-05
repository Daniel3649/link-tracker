package backend.academy.linktracker.scrapper.clients.github;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryIssueResponse;
import backend.academy.linktracker.scrapper.domains.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitHubClientTest {

    private WireMockServer wireMock;
    private GitHubClient gitHubClient;

    @BeforeAll
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        gitHubClient = new GitHubClient(RestClient.builder().baseUrl(wireMock.baseUrl()).build());
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @AfterAll
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldFetchRepositoryStatusWhenRepositoryExists() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(
                get(urlEqualTo("/repos/octocat/Hello-World")).willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        var result = gitHubClient.fetchRepository(key);

        assertThat(result.statusCode()).isEqualTo(HttpStatus.OK);
        wireMock.verify(1, getRequestedFor(urlEqualTo("/repos/octocat/Hello-World")));
    }

    @Test
    void shouldReturnRepositoryStatusEvenWhenGithubRespondsWith500() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlEqualTo("/repos/octocat/Hello-World"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        var result = gitHubClient.fetchRepository(key);

        assertThat(result.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturnEmptyIssueListWhenBodyIsNull() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World/issues"))
                .withQueryParam("state", equalTo("all"))
                .withQueryParam("sort", equalTo("created"))
                .withQueryParam("direction", equalTo("desc"))
                .withQueryParam("per_page", equalTo("10"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        var issues = gitHubClient.fetchRecentIssuesAndPullRequests(key, 10);

        assertThat(issues).isEmpty();
    }

    @Test
    void shouldFetchIssuesAndDetectPullRequestMarker() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World/issues"))
                .withQueryParam("state", equalTo("all"))
                .withQueryParam("sort", equalTo("created"))
                .withQueryParam("direction", equalTo("desc"))
                .withQueryParam("per_page", equalTo("10"))
                .willReturn(okJson("""
                        [
                          {
                            "id": 101,
                            "number": 7,
                            "title": "Bug report",
                            "body": "Issue body",
                            "created_at": "2026-04-05T09:00:00Z",
                            "user": { "login": "octocat" }
                          },
                          {
                            "id": 102,
                            "number": 8,
                            "title": "Feature PR",
                            "body": "PR body",
                            "created_at": "2026-04-05T10:00:00Z",
                            "user": { "login": "hubot" },
                            "pull_request": { "url": "https://api.github.com/repos/octocat/Hello-World/pulls/8" }
                          }
                        ]
                        """)));

        var issues = gitHubClient.fetchRecentIssuesAndPullRequests(key, 10);

        assertThat(issues)
                .extracting(GitHubRepositoryIssueResponse::id)
                .containsExactly(101L, 102L);
        assertThat(issues.getFirst().isPullRequest()).isFalse();
        assertThat(issues.get(1).isPullRequest()).isTrue();
    }

    @Test
    void shouldThrowWhenIssuesResponseBodyIsInvalidJson() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World/issues"))
                .withQueryParam("state", equalTo("all"))
                .withQueryParam("sort", equalTo("created"))
                .withQueryParam("direction", equalTo("desc"))
                .withQueryParam("per_page", equalTo("10"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("{not-json}")));

        assertThatThrownBy(() -> gitHubClient.fetchRecentIssuesAndPullRequests(key, 10))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Failed to fetch repository issues");
    }
}
