package backend.academy.linktracker.scrapper.handlers;

import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.exception.link.OrphanTrackingStateException;
import backend.academy.linktracker.scrapper.handlers.link.ParsedLink;
import backend.academy.linktracker.scrapper.handlers.link.ResourceType;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class GitHubLinkHandler implements LinkHandler {
    private final GitHubTrackingStateRepository repository;

    @Override
    public boolean supports(URI uri) {
        return "github.com".equalsIgnoreCase(uri.getHost()) ||
            "www.github.com".equalsIgnoreCase(uri.getHost());
    }

    @Override
    public ParsedLink parse(URI uri) {
        String[] segments = uri.getPath().split("/");

        if (segments.length < 3) {
            throw new UnsupportedLinkException("Incorrect GitHub link: " + uri);
        }

        String owner = segments[1];
        String repo = segments[2];

        return new ParsedLink(
            uri.toString(),
            new GitHubRepositoryKey(owner, repo),
            ResourceType.GITHUB
        );
    }

    @Override
    public void createTrackingState(TrackedLink trackedLink) {
        boolean isExisted = repository.existsByTrackedLink(trackedLink);
        if (isExisted) {
            throw new OrphanTrackingStateException("Tracked state already exists");
        }
        repository.save(new GitHubTrackingState(trackedLink));
    }

    @Override
    public void deleteTrackingState(TrackedLink trackedLink) {
        repository.deleteByTrackedLink(trackedLink);
    }
}

