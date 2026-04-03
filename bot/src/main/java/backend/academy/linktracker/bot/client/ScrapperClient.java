package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.exception.chat.ChatAlreadyRegisteredException;
import backend.academy.linktracker.bot.exception.chat.ChatNotRegisteredException;
import backend.academy.linktracker.bot.exception.client.InvalidScrapperRequestException;
import backend.academy.linktracker.bot.exception.client.ScrapperClientException;
import backend.academy.linktracker.bot.exception.client.ScrapperUnavailableException;
import backend.academy.linktracker.bot.exception.link.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exception.link.LinkNotTrackedException;
import backend.academy.linktracker.contract.dto.error.ApiErrorResponse;
import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ScrapperClient {
    private static final String TG_CHAT_ID_HEADER = "Tg-Chat-Id";

    private final RestClient scrapperRestClient;
    private final ObjectMapper objectMapper;

    public void registerChat(long chatId) {
        try {
            scrapperRestClient
                    .post()
                    .uri("/tg-chat/{id}", chatId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw mapRegisterChatException(response);
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new ScrapperUnavailableException("Scrapper is unavailable", e);
        }
    }

    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        try {
            LinkResponse response = scrapperRestClient
                    .post()
                    .uri("/links")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TG_CHAT_ID_HEADER, String.valueOf(chatId))
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, responseSpec) -> {
                        throw mapAddLinkException(responseSpec);
                    })
                    .body(LinkResponse.class);

            if (response == null) {
                throw new ScrapperClientException("Scrapper returned empty response body");
            }

            return response;
        } catch (ResourceAccessException e) {
            throw new ScrapperUnavailableException("Scrapper is unavailable", e);
        }
    }

    public LinkResponse removeLink(long chatId, RemoveLinkRequest request) {
        try {
            LinkResponse response = scrapperRestClient
                    .method(HttpMethod.DELETE)
                    .uri("/links")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TG_CHAT_ID_HEADER, String.valueOf(chatId))
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, responseSpec) -> {
                        throw mapRemoveLinkException(responseSpec);
                    })
                    .body(LinkResponse.class);

            if (response == null) {
                throw new ScrapperClientException("Scrapper returned empty response body");
            }

            return response;
        } catch (ResourceAccessException e) {
            throw new ScrapperUnavailableException("Scrapper is unavailable", e);
        }
    }

    public ListLinksResponse getLinks(long chatId) {
        try {
            ListLinksResponse response = scrapperRestClient
                    .get()
                    .uri("/links")
                    .header(TG_CHAT_ID_HEADER, String.valueOf(chatId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, responseSpec) -> {
                        throw mapGetLinksException(responseSpec);
                    })
                    .body(ListLinksResponse.class);

            if (response == null) {
                throw new ScrapperClientException("Scrapper returned empty response body");
            }

            return response;
        } catch (ResourceAccessException e) {
            throw new ScrapperUnavailableException("Scrapper is unavailable", e);
        }
    }

    private RuntimeException mapRegisterChatException(ClientHttpResponse response) throws IOException {
        ApiErrorResponse error = readError(response);
        HttpStatusCode statusCode = response.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message = extractMessage(error, statusCode);

        return switch (status) {
            case BAD_REQUEST -> new InvalidScrapperRequestException(message);
            case CONFLICT -> new ChatAlreadyRegisteredException(message);
            default -> new ScrapperUnavailableException("Unexpected scrapper response. HTTP status: " +
                statusCode.value());
        };
    }

    private RuntimeException mapAddLinkException(ClientHttpResponse response) throws IOException {
        ApiErrorResponse error = readError(response);
        HttpStatusCode statusCode = response.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message = extractMessage(error, statusCode);

        return switch (status) {
            case BAD_REQUEST -> new InvalidScrapperRequestException(message);
            case NOT_FOUND -> new ChatNotRegisteredException(message);
            case CONFLICT ->  new LinkAlreadyTrackedException(message);
            default -> new ScrapperUnavailableException("Unexpected scrapper response. HTTP status: "
                + statusCode.value());
        };
    }

    private RuntimeException mapRemoveLinkException(ClientHttpResponse response) throws IOException {
        ApiErrorResponse error = readError(response);
        HttpStatusCode statusCode = response.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message = extractMessage(error, statusCode);

        return switch (status) {
            case BAD_REQUEST -> new InvalidScrapperRequestException(message);
            case NOT_FOUND -> new LinkNotTrackedException(message);
            default -> new ScrapperUnavailableException("Unexpected scrapper response. HTTP status: "
                + statusCode.value());
        };
    }

    private RuntimeException mapGetLinksException(ClientHttpResponse response) throws IOException {
        ApiErrorResponse error = readError(response);
        HttpStatusCode statusCode = response.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message = extractMessage(error, statusCode);

        return switch (status) {
            case BAD_REQUEST -> new InvalidScrapperRequestException(message);
            case NOT_FOUND -> new ChatNotRegisteredException(message);
            default -> new ScrapperUnavailableException("Unexpected scrapper response. HTTP status: " +
                statusCode.value());
        };
    }

    private ApiErrorResponse readError(ClientHttpResponse response) throws IOException {
        try (InputStream body = response.getBody()) {
            return objectMapper.readValue(body, ApiErrorResponse.class);
        }
    }

    private String extractMessage(ApiErrorResponse error, HttpStatusCode statusCode) {
        if (error != null
                && error.exceptionMessage() != null
                && !error.exceptionMessage().isBlank()) {
            return error.exceptionMessage();
        }
        if (error != null && error.description() != null && !error.description().isBlank()) {
            return error.description();
        }
        return "Scrapper request failed. HTTP status: " + statusCode.value();
    }
}
