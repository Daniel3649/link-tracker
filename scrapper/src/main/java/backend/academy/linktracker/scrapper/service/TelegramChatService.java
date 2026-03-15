package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.chat.TelegramChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramChatService {
    private final TelegramChatRepository telegramChatRepository;

    private final ConcurrentMap<Long, Object> chatRegistrationLocks = new ConcurrentHashMap<>();

    public void registerChat(long chatId) {
        Object lock = chatRegistrationLocks.computeIfAbsent(chatId, ignored -> new Object());

        try {
            MDC.put("chatId", String.valueOf(chatId));

            log.atInfo()
                    .addKeyValue("event", LogEvent.TELEGRAM_CHAT_REGISTER_STARTED)
                    .log("Telegram chat registration started");

            synchronized (lock) {
                if (telegramChatRepository.existsByChatId(chatId)) {
                    log.atWarn()
                            .addKeyValue("event", LogEvent.TELEGRAM_CHAT_REGISTER_FAILED)
                            .addKeyValue("reason", "chat_already_exists")
                            .log("Telegram chat registration rejected");

                    throw new TelegramChatAlreadyExistsException("Telegram chat already exists. Id: " + chatId);
                }

                telegramChatRepository.save(new TelegramChat(chatId));

                log.atInfo()
                        .addKeyValue("event", LogEvent.TELEGRAM_CHAT_REGISTERED)
                        .log("Telegram chat registered");
            }
        } finally {
            MDC.clear();
        }
    }

    public void unregisterChat(long chatId) {
        Object lock = chatRegistrationLocks.computeIfAbsent(chatId, ignored -> new Object());

        try {
            MDC.put("chatId", String.valueOf(chatId));

            log.atInfo()
                    .addKeyValue("event", LogEvent.TELEGRAM_CHAT_UNREGISTER_STARTED)
                    .log("Telegram chat unregistration started");

            synchronized (lock) {
                if (!telegramChatRepository.existsByChatId(chatId)) {
                    log.atWarn()
                            .addKeyValue("event", LogEvent.TELEGRAM_CHAT_UNREGISTER_FAILED)
                            .addKeyValue("reason", "chat_not_found")
                            .log("Telegram chat unregistration rejected");

                    throw new TelegramChatNotFoundException("Telegram chat not found. Id: " + chatId);
                }

                telegramChatRepository.deleteByChatId(chatId);

                log.atInfo()
                        .addKeyValue("event", LogEvent.TELEGRAM_CHAT_UNREGISTERED)
                        .log("Telegram chat unregistered");
            }
        } finally {
            MDC.clear();
        }
    }
}
