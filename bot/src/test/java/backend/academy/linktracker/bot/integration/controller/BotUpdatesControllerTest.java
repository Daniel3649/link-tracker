package backend.academy.linktracker.bot.integration.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableWireMock
class BotUpdatesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @InjectWireMock
    private WireMockServer wireMock;

    @Test
    void shouldAcceptValidUpdateAndSendMessagesToTelegram() throws Exception {
        wireMock.stubFor(post(urlPathMatching("/bot[^/]+/sendMessage")).willReturn(okJson("""
                    {
                      "ok": true,
                      "result": {
                        "message_id": 1
                      }
                    }
                    """)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/updates")
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "id": 1,
                      "url": "https://github.com/octocat/Hello-World",
                      "description": "Repository was updated",
                      "tgChatIds": [1001, 1002]
                    }
                    """))
                .andExpect(status().isOk());

        wireMock.verify(2, postRequestedFor(urlPathMatching(".*/sendMessage")));

        List<LoggedRequest> requests = wireMock.findAll(postRequestedFor(urlPathMatching(".*/sendMessage")));

        assertEquals(2, requests.size());

        String allBodies = requests.stream().map(LoggedRequest::getBodyAsString).collect(Collectors.joining("\n"));

        assertTrue(allBodies.contains("1001"));
        assertTrue(allBodies.contains("1002"));
        assertTrue(allBodies.contains("Hello-World"));
    }

    @Test
    void shouldRejectInvalidUpdateAndNotCallTelegram() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/updates")
                        .contentType(APPLICATION_JSON)
                        .content("""
                    {
                      "id": "wrong-type",
                      "url": "not-a-uri",
                      "description": 123,
                      "tgChatIds": "wrong"
                    }
                    """))
                .andExpect(result -> assertNotEquals(
                        HttpStatus.OK.value(), result.getResponse().getStatus()));

        wireMock.verify(0, postRequestedFor(urlPathMatching(".*/sendMessage")));
    }
}
