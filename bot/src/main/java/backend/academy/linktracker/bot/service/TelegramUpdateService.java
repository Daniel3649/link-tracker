package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import backend.academy.linktracker.bot.exception.handler.dispatcher.ExceptionHandlerDispatcher;
import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.update.TelegramUpdateDispatcher;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramUpdateService {
    private final TelegramUpdateDispatcher telegramUpdateDispatcher;

    public void handleEvent(Update update) {
        if (update == null) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.UPDATE_IGNORED)
                    .addKeyValue("reason", "update_null")
                    .log("Update ignored");
            return;
        }

        Message message = update.message();
        if (message == null) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.UPDATE_IGNORED)
                    .addKeyValue("reason", "message_null")
                    .log("Update ignored");
            return;
        }

        String messageText = message.text();
        if (messageText == null) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.UPDATE_IGNORED)
                    .addKeyValue("reason", "text_null")
                    .log("Update ignored");
            return;
        }

        telegramUpdateDispatcher.dispatch(update);
    }
}
