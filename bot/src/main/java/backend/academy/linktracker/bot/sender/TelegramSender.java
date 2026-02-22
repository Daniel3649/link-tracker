package backend.academy.linktracker.bot.sender;

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
                    .addKeyValue("event", "telegram_send_skipped")
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
                    .addKeyValue("event", "error_sending_response")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("errorCode", errorCode)
                    .addKeyValue("description", description)
                    .log("Error sending response");
        }
    }
}
