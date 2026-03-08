package backend.academy.linktracker.scrapper.handlers;

import backend.academy.linktracker.scrapper.clients.github.GitHubClient;
import backend.academy.linktracker.scrapper.clients.github.dto.GitHubRepositoryFetchResult;
import backend.academy.linktracker.scrapper.exception.link.RepositoryPollingException;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.exception.link.TrackingStateAlreadyExistsException;
import backend.academy.linktracker.scrapper.handlers.common.LinkChange;
import backend.academy.linktracker.scrapper.handlers.common.ParsedLink;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.GitHubTrackingState;
import backend.academy.linktracker.scrapper.repository.GitHubTrackingStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GitHubLinkHandler implements LinkHandler {
    private final GitHubClient gitHubClient;
    private final GitHubTrackingStateRepository trackingStateRepository;

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        return "github.com".equalsIgnoreCase(host)
            || "www.github.com".equalsIgnoreCase(host);
    }

    @Override
    public ParsedLink parse(URI uri) {
        String[] segments = uri.getPath().split("/");

        if (segments.length < 3 ||
            !StringUtils.hasText(segments[1]) ||
            !StringUtils.hasText(segments[2])) {
            throw new UnsupportedLinkException("Incorrect GitHub link: " + uri);
        }

        String owner = segments[1];
        String repo = segments[2];

        return new ParsedLink(
            uri.toString(),
            new GitHubRepositoryKey(owner, repo)
        );
    }

    @Override
    public void createTrackingState(TrackedLink trackedLink) {
        GitHubRepositoryKey key = extractKey(trackedLink.getResourceKey());

        GitHubRepositoryFetchResult result = gitHubClient.fetchRepository(key, null);
        if (!result.isOk()) {
            throw new RepositoryPollingException(
                "Failed to initialize GitHub tracking state for %s. HTTP status: %s"
                    .formatted(trackedLink.getUrl(), result.statusCode().value())
            );
        }

        String etag = requireEtag(result, trackedLink.getUrl());

        GitHubTrackingState state = new GitHubTrackingState(trackedLink);
        state.setEtag(etag);

        boolean saved = trackingStateRepository.saveIfAbsent(state);
        if (!saved) {
            throw new TrackingStateAlreadyExistsException(
                "Tracking state already exists for link: " + trackedLink.getUrl()
            );
        }
    }

    @Override
    public void deleteTrackingState(TrackedLink trackedLink) {
        trackingStateRepository.deleteByTrackedLink(trackedLink);
    }

    @Override
    public Optional<LinkChange> checkForUpdate(TrackedLink trackedLink) {
        Optional<GitHubTrackingState> optionalState = trackingStateRepository.findByTrackedLink(trackedLink);

        if (optionalState.isEmpty()) {
            return Optional.empty();
        }

        GitHubTrackingState state = optionalState.get();
        GitHubRepositoryKey key = extractKey(trackedLink.getResourceKey());

        String oldEtag = state.getEtag();
        GitHubRepositoryFetchResult result = gitHubClient.fetchRepository(key, oldEtag);

        if (result.isNotModified()) {
            return Optional.empty();
        }

        if (result.isNotFound()) {
            return Optional.of(new LinkChange(
                "Repository is unavailable: " + key.owner() + "/" + key.repo()
            ));
        }

        if (!result.isOk()) {
            throw new RepositoryPollingException(
                "Failed to check GitHub repository %s. HTTP status: %s"
                    .formatted(trackedLink.getUrl(), result.statusCode().value())
            );
        }

        String newEtag = requireEtag(result, trackedLink.getUrl());

        if (Objects.equals(oldEtag, newEtag)) {
            return Optional.empty();
        }

        state.setEtag(newEtag);
        trackingStateRepository.save(state);

        return Optional.of(new LinkChange(
            "Repository changed: " + key.owner() + "/" + key.repo()
        ));
    }

    private GitHubRepositoryKey extractKey(ResourceKey resourceKey) {
        if (!(resourceKey instanceof GitHubRepositoryKey key)) {
            throw new IllegalArgumentException("Resource key is not GitHubRepositoryKey");
        }
        return key;
    }

    private String requireEtag(GitHubRepositoryFetchResult result, String url) {
        if (!StringUtils.hasText(result.etag())) {
            throw new RepositoryPollingException(
                "GitHub response does not contain ETag for link: " + url
            );
        }
        return result.etag();
    }
}

