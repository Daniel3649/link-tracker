package backend.academy.linktracker.scrapper.clients.github.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public record GitHubRepositoryFetchResult(HttpStatusCode statusCode, String etag) {
    public boolean isOk() {
        return statusCode.is2xxSuccessful();
    }

    public boolean isNotModified() {
        return HttpStatus.NOT_MODIFIED.equals(statusCode);
    }
}
