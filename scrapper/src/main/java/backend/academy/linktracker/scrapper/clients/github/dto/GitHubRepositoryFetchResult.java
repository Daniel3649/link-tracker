package backend.academy.linktracker.scrapper.clients.github.dto;

import org.springframework.http.HttpStatusCode;

public record GitHubRepositoryFetchResult(HttpStatusCode statusCode, String etag) {
    public boolean isOk() {
        return statusCode.value() == 200;
    }

    public boolean isNotModified() {
        return statusCode.value() == 304;
    }

    public boolean isNotFound() {
        return statusCode.value() == 404;
    }
}
