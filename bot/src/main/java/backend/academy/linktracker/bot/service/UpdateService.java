package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.sender.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateService {
    private final CommandDispatcher commandDispatcher;
    private final MessageService messageService;
    private final TelegramSender sender;

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

        String raw = messageText.strip();
        if (!raw.startsWith("/")) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.UPDATE_IGNORED)
                    .addKeyValue("reason", "not_a_command")
                    .log("Update ignored");
            return;
        }

        String commandToken = raw.split("\\s+", 2)[0]; // "/start@MyBot hello" -> "/start@MyBot"
        String withoutSlash = commandToken.substring(1); // "/start@MyBot" -> "start@MyBot"
        String[] parts = withoutSlash.split("@", 2); // "start@MyBot" -> ["start", "MyBot"]
        String commandName = parts[0].toLowerCase(); // ["start", "MyBot"] -> "start"

        long chatId = update.message().chat().id();
        long updateId = update.updateId();

        try {
            MDC.put("chatId", String.valueOf(chatId));
            MDC.put("updateId", String.valueOf(updateId));
            MDC.put("commandName", commandName);

            log.atInfo().addKeyValue("event", LogEvent.COMMAND_RECEIVED).log("Command received");

            commandDispatcher
                    .getCommandByName(commandName)
                    .ifPresentOrElse(
                            command -> {
                                log.atInfo()
                                        .addKeyValue("event", LogEvent.COMMAND_DISPATCH)
                                        .addKeyValue(
                                                "handler", command.getClass().getSimpleName())
                                        .log("Dispatching command");

                                command.execute(update);

                                log.atInfo()
                                        .addKeyValue("event", LogEvent.COMMAND_HANDLED)
                                        .log("Command handled");
                            },
                            () -> {
                                sender.sendPlain(chatId, messageService.get("command.unknown"));
                                log.atWarn()
                                        .addKeyValue("event", LogEvent.UNKNOWN_COMMAND)
                                        .log("Unknown command");
                            });
        } finally {
            MDC.clear();
        }
    }
}
