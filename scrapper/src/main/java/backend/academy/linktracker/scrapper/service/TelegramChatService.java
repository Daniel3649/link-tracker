package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.chat.TelegramChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramChatService {
    private final TelegramChatRepository telegramChatRepository;

    private final ConcurrentMap<Long, Object> chatRegistrationLocks = new ConcurrentHashMap<>();

    public void registerChat(long chatId) {
        Object lock = chatRegistrationLocks.computeIfAbsent(chatId, ignored -> new Object());

        log.atInfo()
            .addKeyValue("event", "telegram_chat_register_started")
            .addKeyValue("chatId", chatId)
            .log("Telegram chat registration started");

        synchronized (lock) {
            if (telegramChatRepository.existsByChatId(chatId)) {
                log.atWarn()
                    .addKeyValue("event", "telegram_chat_register_rejected")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("reason", "chat_already_exists")
                    .log("Telegram chat registration rejected");

                throw new TelegramChatAlreadyExistsException("Telegram chat already exists. Id: " + chatId);
            }

            telegramChatRepository.save(new TelegramChat(chatId));

            log.atInfo()
                .addKeyValue("event", "telegram_chat_registered")
                .addKeyValue("chatId", chatId)
                .log("Telegram chat registered");
        }
    }

    public void unregisterChat(long chatId) {
        Object lock = chatRegistrationLocks.computeIfAbsent(chatId, ignored -> new Object());

        log.atInfo()
            .addKeyValue("event", "telegram_chat_unregister_started")
            .addKeyValue("chatId", chatId)
            .log("Telegram chat unregistration started");

        synchronized (lock) {
            if (!telegramChatRepository.existsByChatId(chatId)) {
                log.atWarn()
                    .addKeyValue("event", "telegram_chat_unregister_rejected")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("reason", "chat_not_found")
                    .log("Telegram chat unregistration rejected");

                throw new TelegramChatNotFoundException("Telegram chat not found. Id: " + chatId);
            }

            telegramChatRepository.deleteByChatId(chatId);

            log.atInfo()
                .addKeyValue("event", "telegram_chat_unregistered")
                .addKeyValue("chatId", chatId)
                .log("Telegram chat unregistered");
        }
    }
}
