package backend.academy.linktracker.scrapper.clients.github.dto;

import org.springframework.http.HttpStatusCode;

public record GitHubRepositoryFetchResult(HttpStatusCode statusCode) {
    public boolean isOk() {
        return statusCode.is2xxSuccessful();
    }
}
