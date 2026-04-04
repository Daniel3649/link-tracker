package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.support.message.UpdateMessageBuilder;
import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkUpdateNotificationService {
    private final TelegramSender telegramSender;
    private final UpdateMessageBuilder updateMessageBuilder;

    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void sendNotification(LinkUpdate update) {
        Objects.requireNonNull(update, "update cannot be null");

        try (var linkIdMdc = MDC.putCloseable("linkId", String.valueOf(update.id()));
                var urlMdc = MDC.putCloseable("url", update.url().toString())) {
            String message = updateMessageBuilder.buildMessage(update);

            for (Long chatId : update.tgChatIds()) {
                try (var chatIdMdc = MDC.putCloseable("chatId", String.valueOf(chatId))) {
                    telegramSender.sendPlain(chatId, message);
                }
            }

            log.atInfo()
                    .addKeyValue("event", LogEvent.LINK_UPDATE_NOTIFICATION)
                    .addKeyValue("recipientsCount", update.tgChatIds().size())
                    .log("Link update processed");
        }
    }
}
