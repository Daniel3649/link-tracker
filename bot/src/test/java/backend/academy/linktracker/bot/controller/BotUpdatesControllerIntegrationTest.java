package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.exception.handler.BotApiExceptionHandler;
import backend.academy.linktracker.bot.service.LinkUpdateNotificationService;
import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BotUpdatesControllerIntegrationTest {

    @Mock
    private LinkUpdateNotificationService linkUpdateNotificationService;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();

        LinkUpdateController controller =
            new LinkUpdateController(linkUpdateNotificationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
            .setControllerAdvice(new BotApiExceptionHandler())
            .build();
    }

    @Test
    void shouldReturn200ForValidUpdateRequest() throws Exception {
        LinkUpdate request = new LinkUpdate(
            1L,
            URI.create("https://github.com/octocat/Hello-World"),
            "Repository was updated",
            List.of(123L, 456L)
        );

        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(linkUpdateNotificationService).process(request);
    }

    @Test
    void shouldReturnNon200ForInvalidUpdateRequest() throws Exception {
        String invalidJson = """
            {
              "id": "wrong-type",
              "url": "not-a-uri",
              "description": 123,
              "tgChatIds": "wrong"
            }
            """;

        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(linkUpdateNotificationService);
    }
}
