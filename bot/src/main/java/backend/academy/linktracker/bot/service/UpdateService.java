package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
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

    public void handleEvent(Update update) {
        Objects.requireNonNull(update, "Event info is null");

        Message message = update.message();
        if (Objects.isNull(message)) { return; }

        String messageText = message.text();
        if (Objects.isNull(messageText)) { return; }

        commandDispatcher.getCommandByName(messageText.strip())
            .ifPresent(command -> command.execute(update));
    }
}
