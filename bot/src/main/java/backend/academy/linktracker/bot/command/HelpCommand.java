package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {
    private final MessageService messageService;
    private final ObjectProvider<CommandDispatcher> dispatcherProvider;
    private final TelegramSender sender;

    @Override
    public void execute(Update update) {
        Objects.requireNonNull(update);

        StringBuilder message = new StringBuilder();
        message.append(messageService.get("command.help.header")).append('\n');

        CommandDispatcher dispatcher = dispatcherProvider.getObject();

        for (Command command : dispatcher.getCommands()) {
            message.append("/")
                .append(command.name()).append(" - ")
                .append(command.description()).append('\n');
        }

        long chatId = update.message().chat().id();
        sender.sendPlain(chatId, message.toString());
    }

    @Override
    public String name() {
        return CommandName.HELP.getText();
    }

    @Override
    public String description() {
        return messageService.get("command.help.description");
    }
}
