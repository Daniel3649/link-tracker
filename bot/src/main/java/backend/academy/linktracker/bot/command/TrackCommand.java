package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.service.TrackConversationService;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TrackCommand implements Command {
    private final TrackConversationService trackConversationService;
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public void execute(Update update) {
        Objects.requireNonNull(update);

        long chatId = update.message().chat().id();
        trackConversationService.start(chatId);

        telegramSender.sendPlain(
            chatId,
            messageService.get("command.track")
        );
    }

    @Override
    public String name() {
        return CommandName.TRACK.name();
    }

    @Override
    public String description() {
        return messageService.get("command.track.description");
    }
}
