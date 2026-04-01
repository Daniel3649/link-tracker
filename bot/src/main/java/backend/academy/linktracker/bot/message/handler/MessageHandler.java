package backend.academy.linktracker.bot.message.handler;

import backend.academy.linktracker.bot.tracksession.DialogueState;

public interface MessageHandler {
    boolean supports(DialogueState state);

    void handle(long chatId, String message);
}
