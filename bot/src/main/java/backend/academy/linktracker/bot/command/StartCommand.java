package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
    private final TelegramBot bot;
    private final MessageService messageService;
    private final TelegramSender sender;

    @Override
    public void execute(Update update) {
        if (update == null) {throw new IllegalArgumentException("Update object is null");}
        long chatId = update.message().chat().id();
        String message = messageService.get("command.start");
        sender.sendPlain(chatId, message);
    }

    @Override
    public String name() {
        return CommandName.START.getText();
    }

    @Override
    public String description() {
        return messageService.get("command.start.description");
    }
}
