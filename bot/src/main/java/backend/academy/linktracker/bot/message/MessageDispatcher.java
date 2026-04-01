package backend.academy.linktracker.bot.message;

import backend.academy.linktracker.bot.message.handler.MessageHandler;
import backend.academy.linktracker.bot.service.TrackConversationService;
import backend.academy.linktracker.bot.tracksession.DialogueState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
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
    }
}
