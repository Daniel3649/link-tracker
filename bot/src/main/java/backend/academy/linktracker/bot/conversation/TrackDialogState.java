package backend.academy.linktracker.bot.conversation;

import java.net.URI;

public record TrackDialogState(TrackStep step, URI link) {
    public static TrackDialogState waitingLink() {
        return new TrackDialogState(TrackStep.WAITING_LINK, null);
    }

    public static TrackDialogState waitingTags(URI link) {
        return new TrackDialogState(TrackStep.WAITING_TAGS, link);
    }
}
