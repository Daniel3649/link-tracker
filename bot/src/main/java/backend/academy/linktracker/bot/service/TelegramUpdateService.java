package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import backend.academy.linktracker.bot.exception.command.dispatcher.ExceptionHandlerDispatcher;
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
public class TelegramUpdateService {
    private final CommandDispatcher commandDispatcher;
    private final MessageService messageService;
    private final TelegramSender sender;
    private final TrackConversationService trackConversationService;
    private final ExceptionHandlerDispatcher exceptionHandlerDispatcher;

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
        long chatId = message.chat().id();
        long updateId = update.updateId();

        try {
            MDC.put("chatId", String.valueOf(chatId));
            MDC.put("updateId", String.valueOf(updateId));

            if (!raw.startsWith("/")) {
                boolean handled = trackConversationService.handleDialogMessage(chatId, raw);

                if (!handled) {
                    log.atWarn()
                            .addKeyValue("event", LogEvent.UPDATE_IGNORED)
                            .addKeyValue("reason", "not_a_command_and_no_active_dialog")
                            .log("Update ignored");
                }

                return;
            }

            String commandName = extractCommandName(raw);
            MDC.put("commandName", commandName);

            if (trackConversationService.hasActiveSession(chatId) && !"cancel".equals(commandName)) {
                trackConversationService.cancel(chatId);
            }

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

                                try {
                                    command.execute(update);
                                } catch (Exception e) {
                                    exceptionHandlerDispatcher.handle(e, chatId);
                                }

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

    private String extractCommandName(String raw) {
        String commandToken = raw.split("\\s+", 2)[0];
        String withoutSlash = commandToken.substring(1);
        String[] parts = withoutSlash.split("@", 2);
        return parts[0].toLowerCase();
    }
}
