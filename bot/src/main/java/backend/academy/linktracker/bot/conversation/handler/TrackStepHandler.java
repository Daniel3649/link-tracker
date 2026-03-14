package backend.academy.linktracker.bot.conversation.handler;

import backend.academy.linktracker.bot.conversation.TrackDialogState;

public interface TrackStepHandler {
    boolean supports(TrackDialogState state);

    void handle(long chatId, String rawText, TrackDialogState state);
}
