package backend.academy.linktracker.scrapper.clients.bot;

import backend.academy.linktracker.contract.dto.error.ApiErrorResponse;
import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
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

                    String responseBody = readBodySafely(response);
                    logErrorResponse(request, response, responseBody);

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

    private void logErrorResponse(HttpRequest request, ClientHttpResponse response, String body) throws IOException {
        log.atError()
            .addKeyValue("event", LogEvent.BOT_EXCEPTION)
            .addKeyValue("method", request.getMethod())
            .addKeyValue("url", request.getURI().toString())
            .addKeyValue("statusCode", response.getStatusCode().value())
            .addKeyValue("statusText", response.getStatusText())
            .addKeyValue("headers", response.getHeaders())
            .addKeyValue("body", body)
            .log("Bot returned error response");
    }

    private String readBodySafely(ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<failed to read response body: " + e.getMessage() + ">";
        }
    }

    private ApiErrorResponse readError(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
        try {
            return objectMapper.readValue(response.getBody(), ApiErrorResponse.class);
        } catch (IOException e) {
            throw new BotClientException("Failed to read bot error response", e);
        }
    }
}
