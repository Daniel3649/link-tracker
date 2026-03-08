package backend.academy.linktracker.scrapper.handlers;

import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.exception.link.TrackingStateAlreadyExistsException;
import backend.academy.linktracker.scrapper.handlers.common.ParsedLink;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.StackOverflowTrackingState;
import backend.academy.linktracker.scrapper.repository.StackOverflowTrackingStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class StackOverflowLinkParser implements LinkHandler{
    private final StackOverflowTrackingStateRepository repository;

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        return "stackoverflow.com".equalsIgnoreCase(host)
            || "www.stackoverflow.com".equalsIgnoreCase(host);
    }

    @Override
    public ParsedLink parse(URI uri) {
        String[] segments = uri.getPath().split("/");

        if (segments.length < 3 || !"questions".equals(segments[1])) {
            throw new UnsupportedLinkException("Incorrect StackOverflow link: " + uri);
        }

        long questionId;
        try {
            questionId = Long.parseLong(segments[2]);
        } catch (NumberFormatException e) {
            throw new UnsupportedLinkException("Incorrect questionId in link: " + uri, e);
        }

        return new ParsedLink(
            uri.toString(),
            new StackOverflowQuestionKey(questionId)
        );
    }

    @Override
    public void createTrackingState(TrackedLink trackedLink) {
        boolean isExisted = repository.existsByTrackedLink(trackedLink);
        if (isExisted) {
            throw new TrackingStateAlreadyExistsException("Tracked state already exists");
        }

        repository.save(new StackOverflowTrackingState(trackedLink));
    }

    @Override
    public void deleteTrackingState(TrackedLink trackedLink) {
        repository.deleteByTrackedLink(trackedLink);
    }
}
