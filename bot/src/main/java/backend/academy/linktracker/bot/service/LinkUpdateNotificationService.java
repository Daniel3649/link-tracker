package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.support.message.UpdateMessageBuilder;
import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkUpdateNotificationService {
    private final TelegramSender telegramSender;
    private final UpdateMessageBuilder updateMessageBuilder;

    public void sendNotification(LinkUpdate update) {
        Objects.requireNonNull(update, "update cannot be null");

        String message = updateMessageBuilder.buildMessage(update);

        for (Long chatId : update.tgChatIds()) {
            telegramSender.sendPlain(chatId, message);
        }

        log.atInfo()
                .addKeyValue("event", "link_update_notification")
                .addKeyValue("linkId", update.id())
                .addKeyValue("url", update.url())
                .addKeyValue("recipientsCount", update.tgChatIds().size())
                .log("Link update processed");
    }
}
