package backend.academy.linktracker.bot.message;

import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.message.handler.MessageHandler;
import backend.academy.linktracker.bot.service.TrackConversationService;
import backend.academy.linktracker.bot.tracksession.DialogueState;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageDispatcher {
    private final List<MessageHandler> messageHandlers;
    private final TrackConversationService trackConversationService;

    public void dispatch(String message, long chatId) {
        DialogueState dialogueState = trackConversationService.getDialogueState(chatId);

        for (MessageHandler messageHandler : messageHandlers) {
            if (messageHandler.supports(dialogueState)) {
                messageHandler.handle(chatId, message);
                return;
            }
        }

        if (dialogueState != DialogueState.IDLE) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.TRACK_DIALOG_MESSAGE_IGNORED)
                    .addKeyValue("state", dialogueState)
                    .addKeyValue("reason", "no_handler_for_state")
                    .log("Track dialog message ignored");
        }
    }
}
