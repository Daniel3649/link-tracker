package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.service.TrackConversationService;
import com.pengrad.telegrambot.model.Update;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@RequiredArgsConstructor
@Validated
public class TrackCommand implements Command {
    private final TrackConversationService trackConversationService;
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public void execute(@NotNull Update update) {
        long chatId = update.message().chat().id();
        trackConversationService.start(chatId);
        telegramSender.sendPlain(chatId, messageService.get("command.track"));
    }

    @Override
    public String name() {
        return CommandName.TRACK.getText();
    }

    @Override
    public String description() {
        return messageService.get("command.track.description");
    }
}
