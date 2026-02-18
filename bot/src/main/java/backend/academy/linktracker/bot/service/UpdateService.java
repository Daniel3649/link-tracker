package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import backend.academy.linktracker.bot.sender.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateService {
    private final CommandDispatcher commandDispatcher;
    private final MessageService messageService;
    private final TelegramSender sender;

    public void handleEvent(Update update) {
        if (update == null) { return; };

        Message message = update.message();
        if (message == null) { return; }

        String messageText = message.text();
        if (messageText == null) { return; }

        String raw = messageText.strip();
        if (!raw.startsWith("/")) { return; }

        String commandToken = raw.split("\\s+", 2)[0];  // "/start@MyBot hello" -> "/start@MyBot"
        String commandName = commandToken.split("@", 2)[0];  // "/start@MyBot" -> "/start"

        long chatId = message.chat().id();
        commandDispatcher.getCommandByName(commandName)
            .ifPresentOrElse(command -> command.execute(update),
                () -> sender.sendPlain(chatId, messageService.get("command.unknown")));
    }
}
