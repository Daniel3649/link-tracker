package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.command.dispatcher.CommandDispatcher;
import backend.academy.linktracker.bot.sender.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramUpdateService {
    private final CommandDispatcher commandDispatcher;
    private final MessageService messageService;
    private final TelegramSender sender;
    private final TrackConversationService trackConversationService;

    public void handleEvent(Update update) {
        if (update == null) {
            log.atWarn()
                    .addKeyValue("event", "update_ignored")
                    .addKeyValue("reason", "update_null")
                    .log("Update ignored");
            return;
        }

        Message message = update.message();
        if (message == null) {
            log.atWarn()
                    .addKeyValue("event", "update_ignored")
                    .addKeyValue("reason", "message_null")
                    .log("Update ignored");
            return;
        }

        String messageText = message.text();
        if (messageText == null) {
            log.atWarn()
                    .addKeyValue("event", "update_ignored")
                    .addKeyValue("reason", "text_null")
                    .log("Update ignored");
            return;
        }

        String raw = messageText.strip();
        long chatId = message.chat().id();
        long updateId = update.updateId();

        if (!raw.startsWith("/")) {
            boolean handled = trackConversationService.handleDialogMessage(chatId, raw);

            if (!handled) {
                log.atWarn()
                        .addKeyValue("event", "update_ignored")
                        .addKeyValue("reason", "not_a_command_and_no_active_dialog")
                        .addKeyValue("chatId", chatId)
                        .log("Update ignored");
            }

            return;
        }

        String commandName = extractCommandName(raw);

        if (trackConversationService.hasActiveSession(chatId) && !"cancel".equals(commandName)) {
            trackConversationService.cancel(chatId);
        }

        log.atInfo()
                .addKeyValue("event", "command_received")
                .addKeyValue("updateId", updateId)
                .addKeyValue("chatId", chatId)
                .addKeyValue("command", commandName)
                .log("Command received");

        commandDispatcher
                .getCommandByName(commandName)
                .ifPresentOrElse(
                        command -> {
                            log.atInfo()
                                    .addKeyValue("event", "command_dispatch")
                                    .addKeyValue("updateId", updateId)
                                    .addKeyValue("chatId", chatId)
                                    .addKeyValue("command", commandName)
                                    .addKeyValue("handler", command.getClass().getSimpleName())
                                    .log("Dispatching command");

                            command.execute(update);

                            log.atInfo()
                                    .addKeyValue("event", "command_handled")
                                    .addKeyValue("updateId", updateId)
                                    .addKeyValue("chatId", chatId)
                                    .addKeyValue("command", commandName)
                                    .log("Command handled");
                        },
                        () -> {
                            sender.sendPlain(chatId, messageService.get("command.unknown"));
                            log.atWarn()
                                    .addKeyValue("event", "unknown_command")
                                    .addKeyValue("updateId", updateId)
                                    .addKeyValue("chatId", chatId)
                                    .addKeyValue("command", commandName)
                                    .log("Unknown command");
                        });
    }

    private String extractCommandName(String raw) {
        String commandToken = raw.split("\\s+", 2)[0];
        String withoutSlash = commandToken.substring(1);
        String[] parts = withoutSlash.split("@", 2);
        return parts[0].toLowerCase();
    }
}
