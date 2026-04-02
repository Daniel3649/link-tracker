package backend.academy.linktracker.scrapper.clients.github.dto;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;

public record GitHubRepositoryFetchResult(HttpStatusCode statusCode, String etag) {
    public boolean isOk() {
        return HttpStatus.OK.equals(statusCode);
    }

    public boolean isNotModified() {
        return HttpStatus.NOT_MODIFIED.equals(statusCode);
    }
}
