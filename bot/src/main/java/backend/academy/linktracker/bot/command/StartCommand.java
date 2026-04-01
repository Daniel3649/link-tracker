package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.exception.chat.ChatAlreadyRegisteredException;
import backend.academy.linktracker.bot.exception.client.ScrapperClientException;
import backend.academy.linktracker.bot.exception.client.ScrapperUnavailableException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import com.pengrad.telegrambot.model.Update;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@RequiredArgsConstructor
@Validated
public class StartCommand implements Command {
    private final MessageService messageService;
    private final ScrapperClient scrapperClient;
    private final TelegramSender sender;

    @Override
    public void execute(@NotNull Update update) {
        long chatId = update.message().chat().id();
        scrapperClient.registerChat(chatId);
        sender.sendPlain(chatId, messageService.get("command.start"));
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
