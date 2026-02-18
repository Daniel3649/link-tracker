package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.service.MessageService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
    private final TelegramBot bot;
    private final MessageService messageService;

    @Override
    public void execute(Update update) {
        Objects.requireNonNull(update, "Update object is null");
        long chatId = update.message().chat().id();
        var message = new SendMessage(chatId, messageService.get("bot.start"));
        bot.execute(message);
    }

    @Override
    public String name() {
        return CommandName.START.getText();
    }
}
