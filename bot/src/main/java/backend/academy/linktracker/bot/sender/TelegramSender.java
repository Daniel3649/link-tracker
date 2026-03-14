package backend.academy.linktracker.bot.sender;

import backend.academy.linktracker.bot.logging.LogEvent;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramSender {
    private final TelegramBot bot;

    public void sendPlain(long chatId, String message) {
        if (message == null || message.isBlank()) {
            log.atWarn()
                    .addKeyValue("event", LogEvent.TELEGRAM_SEND_SKIPPED)
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("reason", "message_null_or_blank")
                    .log("Send skipped");
            return;
        }

        var sendMessage = new SendMessage(chatId, message);
        BaseResponse response = bot.execute(sendMessage);

        if (!response.isOk()) {
            int errorCode = response.errorCode();
            String description = response.description();

            log.atWarn()
                    .addKeyValue("event", LogEvent.ERROR_SENDING_RESPONSE)
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("errorCode", errorCode)
                    .addKeyValue("description", description)
                    .log("Error sending response");
        }
    }
}
