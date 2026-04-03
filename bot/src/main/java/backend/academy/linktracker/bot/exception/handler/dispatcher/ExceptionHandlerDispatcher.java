package backend.academy.linktracker.bot.exception.handler.dispatcher;

import backend.academy.linktracker.bot.exception.chat.ChatAlreadyRegisteredException;
import backend.academy.linktracker.bot.exception.chat.ChatNotRegisteredException;
import backend.academy.linktracker.bot.exception.client.InvalidScrapperRequestException;
import backend.academy.linktracker.bot.exception.client.ScrapperClientException;
import backend.academy.linktracker.bot.exception.client.ScrapperUnavailableException;
import backend.academy.linktracker.bot.exception.command.NoArgumentUntrackCommandException;
import backend.academy.linktracker.bot.exception.command.UnknownCommandException;
import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.exception.link.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exception.link.LinkNotTrackedException;
import backend.academy.linktracker.bot.exception.link.LinkParsingException;
import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.exception.tracksession.IllegalTrackStateException;
import backend.academy.linktracker.bot.exception.tracksession.TrackSessionNotFoundException;
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
                logHandledException(ex, handler);
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

    private void logHandledException(Exception ex, ExceptionHandler handler) {
        if (isExpectedUserError(ex)) {
            log.atDebug()
                    .addKeyValue("event", LogEvent.HANDLED_EXCEPTION)
                    .addKeyValue("exception", ex.getClass().getSimpleName())
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .addKeyValue("category", "user")
                    .log("Bot exception handled");
            return;
        }

        if (isOperationalWarning(ex)) {
            var warningLog = log.atWarn()
                    .addKeyValue("event", LogEvent.HANDLED_EXCEPTION)
                    .addKeyValue("exception", ex.getClass().getSimpleName())
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .addKeyValue("category", "operational");

            if (ex.getCause() != null) {
                warningLog.setCause(ex);
            }

            warningLog.log("Bot exception handled");
            return;
        }

        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", LogEvent.HANDLED_EXCEPTION)
                .addKeyValue("exception", ex.getClass().getSimpleName())
                .addKeyValue("handler", handler.getClass().getSimpleName())
                .addKeyValue("category", "unexpected")
                .log("Bot exception handled");
    }

    private boolean isExpectedUserError(Exception ex) {
        return ex instanceof UnknownCommandException
                || ex instanceof NoArgumentUntrackCommandException
                || ex instanceof LinkParsingException
                || ex instanceof LinkAlreadyTrackedException
                || ex instanceof LinkNotTrackedException
                || ex instanceof ChatAlreadyRegisteredException
                || ex instanceof ChatNotRegisteredException;
    }

    private boolean isOperationalWarning(Exception ex) {
        return ex instanceof ScrapperUnavailableException
                || ex instanceof ScrapperClientException
                || ex instanceof InvalidScrapperRequestException
                || ex instanceof IllegalTrackStateException
                || ex instanceof TrackSessionNotFoundException;
    }
}
