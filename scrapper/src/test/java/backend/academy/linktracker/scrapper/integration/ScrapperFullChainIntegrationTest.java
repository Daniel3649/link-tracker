package backend.academy.linktracker.scrapper.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@AutoConfigureMockMvc
@EnableWireMock
abstract class ScrapperFullChainIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TelegramChatRepository telegramChatRepository;

    @Autowired
    private TrackedLinkRepository trackedLinkRepository;

    @Autowired
    private SubscriptionTagRepository subscriptionTagRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private GitHubTrackingStateRepository gitHubTrackingStateRepository;

    @Autowired
    private StackOverflowTrackingStateRepository stackOverflowTrackingStateRepository;

    @InjectWireMock
    private WireMockServer wireMock;

    @BeforeEach
    void cleanRepositories() {
        telegramChatRepository.clear();
        trackedLinkRepository.clear();
        subscriptionRepository.clear();
        subscriptionTagRepository.clear();
        gitHubTrackingStateRepository.clear();
        stackOverflowTrackingStateRepository.clear();
    }

    @Test
    void shouldRegisterChatAddGithubLinkAndReturnItInList() throws Exception {
        stubGitHubEndpoints();

        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "link": "https://github.com/octocat/Hello-World",
                      "tags": ["java", "spring"],
                      "filters": []
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").value("https://github.com/octocat/Hello-World"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].id").isNumber())
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/octocat/Hello-World"));

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World")));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/activity")));
    }

    @Test
    void shouldRegisterChatAddAndDeleteGithubLinkAndReturnEmptyList() throws Exception {
        stubGitHubEndpoints();

        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "link": "https://github.com/octocat/Hello-World",
                      "tags": ["java", "spring"],
                      "filters": []
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").value("https://github.com/octocat/Hello-World"));

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "link": "https://github.com/octocat/Hello-World"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").value("https://github.com/octocat/Hello-World"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.links").isArray())
                .andExpect(jsonPath("$.links").isEmpty());

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World")));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/activity")));
    }

    @Test
    void shouldNotDeleteLinkFromNonExistentChatAndLinkShouldRemainInExistingChat() throws Exception {
        stubGitHubEndpoints();

        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://github.com/octocat/Hello-World",
                  "tags": ["java", "spring"],
                  "filters": []
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").value("https://github.com/octocat/Hello-World"));

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 999L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://github.com/octocat/Hello-World"
                }
                """))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].id").isNumber())
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/octocat/Hello-World"));
    }

    @Test
    void shouldNotAddLinkToNonExistentChat() throws Exception {
        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://github.com/octocat/Hello-World",
                  "tags": ["java", "spring"],
                  "filters": []
                }
                """))
                .andExpect(status().is4xxClientError());

        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World")));
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/activity")));
    }

    @Test
    void shouldNotAddLinkToDeletedChat() throws Exception {
        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(delete("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://github.com/octocat/Hello-World",
                  "tags": ["java", "spring"],
                  "filters": []
                }
                """))
                .andExpect(status().is4xxClientError());

        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World")));
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/activity")));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentChat() throws Exception {
        mockMvc.perform(delete("/tg-chat/{id}", 1L)).andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidGithubLink() throws Exception {
        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "jjj://github.com/user/repo",
                  "tags": ["java"],
                  "filters": []
                }
                """))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.links").isArray())
                .andExpect(jsonPath("$.links").isEmpty());

        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/user/repo")));
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/user/repo/activity")));
    }

    @Test
    void shouldRejectDuplicateSubscriptionForSameChat() throws Exception {
        stubGitHubEndpoints();

        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://github.com/octocat/Hello-World",
                  "tags": ["java", "spring"],
                  "filters": []
                }
                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://github.com/octocat/Hello-World",
                  "tags": ["java", "spring"],
                  "filters": []
                }
                """))
                .andExpect(status().isConflict());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/octocat/Hello-World"));
    }

    @Test
    void shouldPersistTagsAndReturnThemInList() throws Exception {
        stubGitHubEndpoints();

        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://github.com/octocat/Hello-World",
                  "tags": ["java", "spring"],
                  "filters": []
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/octocat/Hello-World"))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags", containsInAnyOrder("java", "spring")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/octocat/Hello-World"))
                .andExpect(jsonPath("$.links[0].tags", containsInAnyOrder("java", "spring")));
    }

    @Test
    void shouldRegisterChatAddStackOverflowLinkAndReturnItInList() throws Exception {
        stubStackOverflowEndpoints();

        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                {
                  "link": "https://stackoverflow.com/questions/12345678/example-question",
                  "tags": ["study"],
                  "filters": []
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").value("https://stackoverflow.com/questions/12345678/example-question"))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("study")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].url")
                        .value("https://stackoverflow.com/questions/12345678/example-question"))
                .andExpect(jsonPath("$.links[0].tags", containsInAnyOrder("study")));

        wireMock.verify(
                1,
                getRequestedFor(urlPathEqualTo("/questions/12345678"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .withQueryParam("key", equalTo("test-stackoverflow-key")));

        wireMock.verify(
                1,
                getRequestedFor(urlPathEqualTo("/questions/12345678/timeline"))
                        .withQueryParam("site", equalTo("stackoverflow"))
                        .withQueryParam("key", equalTo("test-stackoverflow-key"))
                        .withQueryParam("pagesize", equalTo("1")));
    }

    private void stubStackOverflowEndpoints() {
        wireMock.stubFor(get(urlPathEqualTo("/questions/12345678"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-stackoverflow-key"))
                .willReturn(okJson("""
            {
              "items": [
                {
                  "question_id": 12345678,
                  "last_activity_date": 1741680000
                }
              ],
              "backoff": 0
            }
            """)));

        wireMock.stubFor(get(urlPathEqualTo("/questions/12345678/timeline"))
                .withQueryParam("site", equalTo("stackoverflow"))
                .withQueryParam("key", equalTo("test-stackoverflow-key"))
                .withQueryParam("pagesize", equalTo("1"))
                .willReturn(okJson("""
            {
              "items": [],
              "backoff": 0
            }
            """)));
    }

    private void stubGitHubEndpoints() {
        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World"))
                .willReturn(aResponse().withStatus(200).withHeader("ETag", "\"test-etag-123\"")));

        wireMock.stubFor(
                get(urlPathEqualTo("/repos/octocat/Hello-World/activity")).willReturn(okJson("""
                        [
                          {
                            "id": 1001,
                            "activity_type": "push",
                            "ref": "refs/heads/main",
                            "before": "1111111111111111111111111111111111111111",
                            "after": "2222222222222222222222222222222222222222",
                            "pushed_at": "2026-03-11T10:15:30Z",
                            "push_type": "branch",
                            "pusher": {
                              "login": "octocat"
                            }
                          }
                        ]
                        """)));
    }
}
