package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.contract.dto.error.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

abstract class AbstractHttpBotSender {
    private static final int BODY_PREVIEW_LIMIT = 200;

    private final RestClient botRestClient;
    private final ObjectMapper objectMapper;
    private final Logger log;

    protected AbstractHttpBotSender(RestClient botRestClient, ObjectMapper objectMapper, Logger log) {
        this.botRestClient = botRestClient;
        this.objectMapper = objectMapper;
        this.log = log;
    }

    protected void sendJson(String path, Object payload, String badRequestMessagePrefix) {
        botRestClient
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange((request, response) -> {
                    HttpStatusCode status = response.getStatusCode();

                    if (status.is2xxSuccessful()) {
                        return null;
                    }

                    String responseBody = readBodySafely(response);
                    ApiErrorResponse error = readError(responseBody);
                    logErrorResponse(request, response, responseBody, error);

                    if (HttpStatus.BAD_REQUEST.equals(status)) {
                        throw new BotClientException(
                                badRequestMessagePrefix + extractMessage(error, status, responseBody));
                    }

                    if (status.is5xxServerError()) {
                        throw new BotClientException("Bot service error. HTTP status: " + status.value());
                    }

                    throw new BotClientException("Unexpected bot response. HTTP status: " + status.value());
                });
    }

    private void logErrorResponse(HttpRequest request, ClientHttpResponse response, String body, ApiErrorResponse error)
            throws IOException {
        var logEntry = log.atError()
                .addKeyValue("event", LogEvent.BOT_RESPONSE_FAILED)
                .addKeyValue("method", request.getMethod())
                .addKeyValue("url", request.getURI().toString())
                .addKeyValue("statusCode", response.getStatusCode().value())
                .addKeyValue("statusText", response.getStatusText());

        if (error != null) {
            if (hasText(error.exceptionName())) {
                logEntry = logEntry.addKeyValue("remoteException", error.exceptionName());
            }
            if (hasText(error.exceptionMessage())) {
                logEntry = logEntry.addKeyValue("remoteMessage", abbreviate(error.exceptionMessage()));
            } else if (hasText(error.description())) {
                logEntry = logEntry.addKeyValue("remoteDescription", abbreviate(error.description()));
            }
        } else if (hasText(body)) {
            logEntry = logEntry.addKeyValue("bodyPreview", abbreviate(body));
        }

        logEntry.log("Bot returned error response");
    }

    private String readBodySafely(ClientHttpResponse response) {
        try {
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<failed to read response body: " + e.getMessage() + ">";
        }
    }

    private ApiErrorResponse readError(String responseBody) {
        if (!hasText(responseBody)) {
            return null;
        }

        try {
            return objectMapper.readValue(responseBody, ApiErrorResponse.class);
        } catch (IOException e) {
            return null;
        }
    }

    private String extractMessage(ApiErrorResponse error, HttpStatusCode status, String responseBody) {
        if (error != null) {
            if (hasText(error.exceptionMessage())) {
                return error.exceptionMessage();
            }
            if (hasText(error.description())) {
                return error.description();
            }
        }

        if (hasText(responseBody)) {
            return abbreviate(responseBody);
        }

        return "Bot request failed. HTTP status: " + status.value();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String abbreviate(String value) {
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= BODY_PREVIEW_LIMIT) {
            return normalized;
        }

        return normalized.substring(0, BODY_PREVIEW_LIMIT) + "...";
    }
}
