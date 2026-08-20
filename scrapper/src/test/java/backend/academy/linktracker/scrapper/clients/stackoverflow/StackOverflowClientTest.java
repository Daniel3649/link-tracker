package backend.academy.linktracker.scrapper.clients.stackoverflow;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.domains.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

class StackOverflowClientTest {

    private WireMockServer wireMock;
    private StackOverflowClient stackOverflowClient;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        StackoverflowProperties properties = mock(StackoverflowProperties.class);
        when(properties.getSite()).thenReturn("stackoverflow");
        when(properties.getKey()).thenReturn("test-stackoverflow-key");

        stackOverflowClient = new StackOverflowClient(
                properties, RestClient.builder().baseUrl(wireMock.baseUrl()).build());
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void shouldFetchQuestionWithSiteAndKey() {
        StackOverflowQuestionKey key = new StackOverflowQuestionKey(123L);

        wireMock.stubFor(get(urlPathEqualTo("/questions/123"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-stackoverflow-key"))
                .willReturn(okJson("""
                {
                  "items": [
                    {
                      "question_id": 123,
                      "last_activity_date": 1741680000
                    }
                  ],
                  "backoff": 5
                }
                """)));

        var result = stackOverflowClient.fetchQuestion(key);

        assertThat(result.question()).isNotNull();
        assertThat(result.question().lastActivityDateEpochSec()).isEqualTo(1741680000L);
        assertThat(result.backoffSeconds()).isEqualTo(5);
    }

    @Test
    void shouldThrowWhenQuestionResponseBodyIsNull() {
        StackOverflowQuestionKey key = new StackOverflowQuestionKey(123L);

        wireMock.stubFor(get(urlPathEqualTo("/questions/123"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        assertThatThrownBy(() -> stackOverflowClient.fetchQuestion(key))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Empty StackOverflow API response");
    }

    @Test
    void shouldThrowWhenQuestionResponseContainsApiError() {
        StackOverflowQuestionKey key = new StackOverflowQuestionKey(123L);

        wireMock.stubFor(get(urlPathEqualTo("/questions/123")).willReturn(okJson("""
                {
                  "error_id": 400,
                  "error_message": "bad request",
                  "error_name": "bad_request",
                  "items": []
                }
                """)));

        assertThatThrownBy(() -> stackOverflowClient.fetchQuestion(key))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("StackOverflow API error");
    }

    @Test
    void shouldFetchTimelineWithPageSize() {
        StackOverflowQuestionKey key = new StackOverflowQuestionKey(123L);

        wireMock.stubFor(get(urlPathEqualTo("/questions/123/timeline"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-stackoverflow-key"))
                .withQueryParam("pagesize", equalTo("10"))
                .willReturn(okJson("""
                {
                  "items": [],
                  "backoff": 7
                }
                """)));

        var result = stackOverflowClient.fetchQuestionTimeline(key, 10);

        assertThat(result.events()).isEmpty();
        assertThat(result.backoffSeconds()).isEqualTo(7);
    }

    @Test
    void shouldFetchAnswerWithBodyAndOwner() {
        wireMock.stubFor(get(urlPathEqualTo("/answers/456"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-stackoverflow-key"))
                .withQueryParam("filter", equalTo("withbody"))
                .willReturn(okJson("""
                {
                  "items": [
                    {
                      "answer_id": 456,
                      "creation_date": 1741680000,
                      "body": "<p>Answer body</p>",
                      "owner": {
                        "display_name": "alice"
                      }
                    }
                  ],
                  "backoff": 3
                }
                """)));

        var result = stackOverflowClient.fetchAnswer(456L);

        assertThat(result.item()).isNotNull();
        assertThat(result.item().answerId()).isEqualTo(456L);
        assertThat(result.item().body()).isEqualTo("<p>Answer body</p>");
        assertThat(result.item().owner().displayName()).isEqualTo("alice");
        assertThat(result.backoffSeconds()).isEqualTo(3);
    }

    @Test
    void shouldFetchCommentWithBodyAndOwner() {
        wireMock.stubFor(get(urlPathEqualTo("/comments/789"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-stackoverflow-key"))
                .withQueryParam("filter", equalTo("withbody"))
                .willReturn(okJson("""
                {
                  "items": [
                    {
                      "comment_id": 789,
                      "creation_date": 1741681234,
                      "body": "<p>Comment body</p>",
                      "owner": {
                        "display_name": "bob"
                      }
                    }
                  ],
                  "backoff": 4
                }
                """)));

        var result = stackOverflowClient.fetchComment(789L);

        assertThat(result.item()).isNotNull();
        assertThat(result.item().commentId()).isEqualTo(789L);
        assertThat(result.item().body()).isEqualTo("<p>Comment body</p>");
        assertThat(result.item().owner().displayName()).isEqualTo("bob");
        assertThat(result.backoffSeconds()).isEqualTo(4);
    }

    @Test
    void shouldThrowWhenTimelineEndpointReturnsServerError() {
        StackOverflowQuestionKey key = new StackOverflowQuestionKey(123L);

        wireMock.stubFor(get(urlPathEqualTo("/questions/123/timeline"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        assertThatThrownBy(() -> stackOverflowClient.fetchQuestionTimeline(key, 10))
                .isInstanceOf(RepositoryPollingException.class)
                .hasMessageContaining("Failed to call StackOverflow timeline API");
    }
}
