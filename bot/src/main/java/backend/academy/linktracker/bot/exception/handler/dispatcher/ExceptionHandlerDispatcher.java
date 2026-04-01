package backend.academy.linktracker.bot.exception.handler.dispatcher;

import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
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
    private final List<ExceptionHandler> handlers;

    public void handle(Exception ex, long chatId) {
        for (ExceptionHandler handler : handlers) {
            if (handler.supports(ex)) {
                handler.handle(ex, chatId);
                return;
            }
        }
        handleUnknownException(chatId);
    }

    private void handleUnknownException(long chatId)  {
        sender.sendPlain(chatId, messageService.get("exception.unknown"));
    }
}
