package backend.academy.linktracker.scrapper.models.link.resourcekey;

public record GitHubRepositoryKey(String owner, String repo) implements ResourceKey {}
