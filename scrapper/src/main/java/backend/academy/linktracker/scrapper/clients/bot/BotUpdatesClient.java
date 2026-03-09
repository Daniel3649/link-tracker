package backend.academy.linktracker.scrapper.clients.bot;

import backend.academy.linktracker.scrapper.dto.error.ApiErrorResponse;
import backend.academy.linktracker.scrapper.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class BotUpdatesClient {
    private final RestClient botRestClient;
    private final ObjectMapper objectMapper;

    public void sendUpdate(LinkUpdate update) {
        botRestClient.post()
            .uri("/updates")
            .contentType(MediaType.APPLICATION_JSON)
            .body(update)
            .exchange((request, response) -> {
                int status = response.getStatusCode().value();

                if (status == 200) {
                    return null;
                }

                if (status == 400) {
                    ApiErrorResponse error = readError(response);
                    throw new BotClientException(
                        "Bot rejected update: " + error.exceptionMessage()
                    );
                }

                if (status >= 500) {
                    throw new BotClientException(
                        "Bot service error. HTTP status: " + status
                    );
                }

                throw new BotClientException(
                    "Unexpected bot response. HTTP status: " + status
                );
            });
    }

    private ApiErrorResponse readError(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        try {
            return objectMapper.readValue(response.getBody(), ApiErrorResponse.class);
        } catch (IOException e) {
            throw new BotClientException("Failed to read bot error response", e);
        }
    }
}
