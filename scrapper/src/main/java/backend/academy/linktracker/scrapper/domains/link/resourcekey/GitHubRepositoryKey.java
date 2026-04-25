package backend.academy.linktracker.scrapper.domains.link.resourcekey;

public record GitHubRepositoryKey(String owner, String repo) implements ResourceKey {}
