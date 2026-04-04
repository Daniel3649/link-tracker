package backend.academy.linktracker.bot.message.handler;

import backend.academy.linktracker.bot.exception.link.LinkParsingException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.service.TrackConversationService;
import backend.academy.linktracker.bot.tracksession.DialogueState;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddLinkMessageHandler implements MessageHandler {
    private final TelegramSender telegramSender;
    private final MessageService messageService;
    private final TrackConversationService trackConversationService;

    @Override
    public boolean supports(DialogueState state) {
        return state == DialogueState.WAITING_LINK;
    }

    @Override
    public void handle(long chatId, String rawText) {
        URI link;
        try {
            link = URI.create(rawText);
        } catch (IllegalArgumentException x) {
            throw new LinkParsingException("Invalid uri: " + rawText, x);
        }

        trackConversationService.acceptLink(chatId, link);
        telegramSender.sendPlain(chatId, messageService.get("link.accept.success"));
    }
}
