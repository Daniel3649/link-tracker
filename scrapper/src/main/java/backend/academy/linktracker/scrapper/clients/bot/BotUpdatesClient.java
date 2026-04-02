package backend.academy.linktracker.scrapper.clients.bot;

import backend.academy.linktracker.contract.dto.error.ApiErrorResponse;
import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class BotUpdatesClient {
    private final RestClient botRestClient;
    private final ObjectMapper objectMapper;

    public void sendUpdate(LinkUpdate update) {
        botRestClient
                .post()
                .uri("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .body(update)
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();

                    if (status.is2xxSuccessful()) {
                        return null;
                    }

                    if (HttpStatus.BAD_REQUEST.equals(status)) {
                        ApiErrorResponse error = readError(response);
                        throw new BotClientException("Bot rejected update: " + error.exceptionMessage());
                    }

                    if (status.is5xxServerError()) {
                        throw new BotClientException("Bot service error. HTTP status: " + status.value());
                    }

                    throw new BotClientException("Unexpected bot response. HTTP status: " + status.value());
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
