package backend.academy.linktracker.bot.sender;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramSender {
    private final TelegramBot bot;

    public void sendPlain(long chatId, String message) {
        var sendMessage = new SendMessage(chatId, message);
        BaseResponse response = bot.execute(sendMessage);
    }
}
