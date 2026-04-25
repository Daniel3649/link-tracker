package backend.academy.linktracker.bot.tracksession;

import java.net.URI;

public record TrackSession(DialogueState state, URI link) {
    public static TrackSession waitingLink() {
        return new TrackSession(DialogueState.WAITING_LINK, null);
    }

    public static TrackSession waitingTags(URI link) {
        return new TrackSession(DialogueState.WAITING_TAGS, link);
    }
}
