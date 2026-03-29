package backend.academy.linktracker.bot.exception.command.dispatcher;

import backend.academy.linktracker.bot.exception.client.InvalidScrapperRequestException;
import backend.academy.linktracker.bot.exception.command.handler.CommandExceptionHandler;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExceptionHandlerDispatcher {
    private final TelegramSender sender;
    private final MessageService messageService;
    private final List<CommandExceptionHandler> handlers;

    public void handle(Exception ex, long chatId) {
        for (CommandExceptionHandler handler : handlers) {
            if (handler.supports(ex)) {
                handler.handle(ex, chatId);
                return;
            }
        }
        handleUnknownException(chatId);
    }

    private void handleUnknownException(long chatId)  {
        sender.sendPlain(chatId, messageService.get("command.exception.unknown"));
    }
}
