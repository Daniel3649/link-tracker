package backend.academy.linktracker.bot.track.handler;

import backend.academy.linktracker.bot.track.TrackDialogState;

public interface TrackStepHandler {
    boolean supports(TrackDialogState state);

    void handle(long chatId, String rawText, TrackDialogState state);
}
