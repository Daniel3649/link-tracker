package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.contract.dto.request.TextNotification;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TextNotificationService {
    private final TelegramSender telegramSender;

    public void sendNotification(TextNotification notification) {
        Objects.requireNonNull(notification, "notification cannot be null");

        for (Long chatId : notification.tgChatIds()) {
            try (var _ = MDC.putCloseable("chatId", String.valueOf(chatId))) {
                telegramSender.sendPlain(chatId, notification.message());
            }
        }

        log.atInfo()
                .addKeyValue("event", LogEvent.TEXT_NOTIFICATION)
                .addKeyValue("recipientsCount", notification.tgChatIds().size())
                .log("Text notification processed");
    }
}
