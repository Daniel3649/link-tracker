package backend.academy.linktracker.scrapper.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

import backend.academy.linktracker.scrapper.clients.github.GitHubClient;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

class GitHubClientTest {

    private WireMockServer wireMock;
    private GitHubClient gitHubClient;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        gitHubClient = new GitHubClient(
                RestClient.builder().baseUrl(wireMock.baseUrl()).build());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldFetchRepositoryAndPassIfNoneMatchHeader() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlEqualTo("/repos/octocat/Hello-World"))
                .withHeader("If-None-Match", equalTo("\"old-etag\""))
                .willReturn(aResponse().withStatus(200).withHeader("ETag", "\"new-etag\"")));

        var result = gitHubClient.fetchRepository(key, "\"old-etag\"");

        assertThat(result.statusCode().value()).isEqualTo(200);
        assertThat(result.etag()).isNotBlank();
        assertThat(result.etag()).contains("new-etag");

        wireMock.verify(
                1,
                getRequestedFor(urlEqualTo("/repos/octocat/Hello-World"))
                        .withHeader("If-None-Match", equalTo("\"old-etag\"")));
    }

    @Test
    void shouldReturnRepositoryStatusEvenWhenGithubRespondsWith500() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlEqualTo("/repos/octocat/Hello-World"))
                .willReturn(aResponse().withStatus(500)));

        var result = gitHubClient.fetchRepository(key, null);

        assertThat(result.statusCode().value()).isEqualTo(500);
        assertThat(result.etag()).isNull();
    }

    @Test
    void shouldReturnEmptyActivityListWhenBodyIsNull() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World/activity"))
                .withQueryParam("direction", equalTo("desc"))
                .withQueryParam("per_page", equalTo("10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        var activities = gitHubClient.fetchRecentActivities(key, 10);

        assertThat(activities).isEmpty();
    }

    @Test
    void shouldThrowWhenActivityResponseBodyIsInvalidJson() {
        GitHubRepositoryKey key = new GitHubRepositoryKey("octocat", "Hello-World");

        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World/activity"))
                .withQueryParam("direction", equalTo("desc"))
                .withQueryParam("per_page", equalTo("10"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{not-json}")));

        assertThatThrownBy(() -> gitHubClient.fetchRecentActivities(key, 10))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Failed to fetch repository activity");
    }
}
