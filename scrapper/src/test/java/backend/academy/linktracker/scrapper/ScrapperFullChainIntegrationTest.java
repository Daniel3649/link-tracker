package backend.academy.linktracker.scrapper;

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

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableWireMock
class ScrapperFullChainIntegrationTest {

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

        mockMvc.perform(post("/tg-chat/{id}", 1L))
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

        mockMvc.perform(post("/tg-chat/{id}", 1L))
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

        mockMvc.perform(post("/tg-chat/{id}", 1L))
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
        mockMvc.perform(post("/tg-chat/{id}", 1L))
            .andExpect(status().isOk());

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
        mockMvc.perform(post("/tg-chat/{id}", 1L))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/tg-chat/{id}", 1L))
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
            .andExpect(status().is4xxClientError());

        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World")));
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/activity")));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentChat() throws Exception {
        mockMvc.perform(delete("/tg-chat/{id}", 1L))
            .andExpect(status().isNotFound());
    }

    private void stubGitHubEndpoints() {
        wireMock.stubFor(
            get(urlPathEqualTo("/repos/octocat/Hello-World"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("ETag", "\"test-etag-123\"")
                )
        );

        wireMock.stubFor(
            get(urlPathEqualTo("/repos/octocat/Hello-World/activity"))
                .willReturn(
                    okJson("""
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
                        """)
                )
        );
    }
}
