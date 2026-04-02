package backend.academy.linktracker.bot.update;

import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import backend.academy.linktracker.bot.exception.handler.dispatcher.ExceptionHandlerDispatcher;
import backend.academy.linktracker.bot.message.MessageDispatcher;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUpdateDispatcher {
    private final MessageDispatcher messageDispatcher;
    private final CommandDispatcher commandDispatcher;
    private final ExceptionHandlerDispatcher exceptionHandlerDispatcher;

    public void dispatch(Update update) {
        String message = update.message().text().strip();
        long chatId = update.message().chat().id();
        long updateId = update.updateId();

        try (var chatIdMdc = MDC.putCloseable("chatId", String.valueOf(chatId));
            var updateIdMdc = MDC.putCloseable("updateId", String.valueOf(updateId))) {
            try {
                if (!message.startsWith("/")) {
                    messageDispatcher.dispatch(message, chatId);
                } else {
                    String command = extractCommandName(message);
                    commandDispatcher.dispatch(command, update);
                }
            } catch (Exception ex) {
                exceptionHandlerDispatcher.handle(ex, chatId);
            }
        }
    }

    private String extractCommandName(String raw) {
        String commandToken = raw.split("\\s+", 2)[0];
        String withoutSlash = commandToken.substring(1);
        String[] parts = withoutSlash.split("@", 2);
        return parts[0].toLowerCase();
    }
}
