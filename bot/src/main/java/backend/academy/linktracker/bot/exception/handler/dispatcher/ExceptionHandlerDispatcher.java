package backend.academy.linktracker.bot.exception.handler.dispatcher;

import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
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
        handleUnknownException(ex, chatId);
    }

    private void handleUnknownException(Exception ex, long chatId) {
        log.atError()
                .setCause(ex)
                .addKeyValue("event", LogEvent.UNHANDLED_EXCEPTION)
                .addKeyValue("exception", ex.getClass().getSimpleName())
                .log("Unhandled bot exception");
        sender.sendPlain(chatId, messageService.get("exception.unknown"));
    }
}
