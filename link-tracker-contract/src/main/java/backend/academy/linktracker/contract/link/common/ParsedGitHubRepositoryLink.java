package backend.academy.linktracker.contract.link.common;

import java.net.URI;
import java.util.Objects;

public record ParsedGitHubRepositoryLink(
    URI uri,
    String owner,
    String repo) implements ParsedSupportedLink {
    public ParsedGitHubRepositoryLink {
        Objects.requireNonNull(uri);
        Objects.requireNonNull(owner);
        Objects.requireNonNull(repo);

        if (owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
        if (repo.isBlank()) {
            throw new IllegalArgumentException("repo must not be blank");
        }
    }

    @Override
    public SupportedLinkKind kind() {
        return SupportedLinkKind.GITHUB_REPOSITORY;
    }
}
