package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.tracksession.CancelTrackResult;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.service.TrackConversationService;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelCommand implements Command {
    private final MessageService messageService;
    private final TrackConversationService trackConversationService;
    private final TelegramSender telegramSender;

    @Override
    public void execute(Update update) {
        long chatId = update.message().chat().id();

        CancelTrackResult result = trackConversationService.cancel(chatId);

        switch (result) {
            case CANCELLED ->
                telegramSender.sendPlain(chatId, messageService.get("command.cancel"));
            case NO_ACTIVE_SESSION ->
                telegramSender.sendPlain(chatId, messageService.get("command.cancel.dialog-not-found"));
        }
    }

    @Override
    public String name() {
        return CommandName.CANCEL.getText();
    }

    @Override
    public String description() {
        return messageService.get("command.cancel.description");
    }
}
