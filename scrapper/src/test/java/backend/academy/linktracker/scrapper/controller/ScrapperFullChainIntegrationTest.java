package backend.academy.linktracker.scrapper.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.integration.AbstractIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@EnableWireMock
abstract class ScrapperFullChainIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @InjectWireMock
    private WireMockServer wireMock;

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
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/issues")));
    }

    @Test
    void shouldManageTagsSeparatelyFromLinks() throws Exception {
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

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tags")
                        .header("Tg-Chat-Id", 1L)
                        .param("link", "https://github.com/octocat/Hello-World"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("java", "spring")));

        mockMvc.perform(post("/tags")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "link": "https://github.com/octocat/Hello-World",
                      "tag": "backend"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value("backend"));

        mockMvc.perform(put("/tags")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "link": "https://github.com/octocat/Hello-World",
                      "currentTag": "spring",
                      "newTag": "spring-boot"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value("spring-boot"));

        mockMvc.perform(delete("/tags")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "link": "https://github.com/octocat/Hello-World",
                      "tag": "java"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value("java"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/tags")
                        .header("Tg-Chat-Id", 1L)
                        .param("link", "https://github.com/octocat/Hello-World"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.tags", containsInAnyOrder("backend", "spring-boot")));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/links")
                        .header("Tg-Chat-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[0].tags", containsInAnyOrder("backend", "spring-boot")));
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
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/issues")));
    }

    @Test
    void shouldRollbackTrackedLinkCreationWhenTrackingStateInitializationFails() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World"))
                .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        mockMvc.perform(post("/tg-chat/{id}", 1L)).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1L)
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "link": "https://github.com/octocat/Hello-World",
                      "tags": ["java"],
                      "filters": []
                    }
                    """))
                .andExpect(status().isBadGateway());

        assertThat(trackedLinkRepository.findAll()).isEmpty();
        assertThat(subscriptionRepository.findAllByTelegramChatId(1L)).isEmpty();

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World")));
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/issues")));
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
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/issues")));
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
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/octocat/Hello-World/issues")));
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
        wireMock.verify(0, getRequestedFor(urlPathEqualTo("/repos/user/repo/issues")));
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
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())));

        wireMock.stubFor(get(urlPathEqualTo("/repos/octocat/Hello-World/issues"))
                .withQueryParam("state", equalTo("all"))
                .withQueryParam("sort", equalTo("created"))
                .withQueryParam("direction", equalTo("desc"))
                .withQueryParam("per_page", equalTo("1"))
                .willReturn(okJson("""
                        []
                        """)));
    }
}
