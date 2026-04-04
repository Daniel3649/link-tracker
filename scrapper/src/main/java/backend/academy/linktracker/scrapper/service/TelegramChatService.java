package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.chat.TelegramChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramChatService {
    private final TelegramChatRepository telegramChatRepository;

    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void registerChat(long chatId) {
        try (var chatIdMdc = MDC.putCloseable("chatId", String.valueOf(chatId))) {
            boolean created = telegramChatRepository.saveIfAbsent(new TelegramChat(chatId));
            if (!created) {
                throw new TelegramChatAlreadyExistsException("Telegram chat already exists. Id: " + chatId);
            }

            log.atInfo().addKeyValue("event", LogEvent.TELEGRAM_CHAT_REGISTERED).log("Telegram chat registered");
        }
    }

    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void unregisterChat(long chatId) {
        try (var chatIdMdc = MDC.putCloseable("chatId", String.valueOf(chatId))) {
            if (telegramChatRepository.removeByChatId(chatId).isEmpty()) {
                throw new TelegramChatNotFoundException("Telegram chat not found. Id: " + chatId);
            }

            log.atInfo()
                    .addKeyValue("event", LogEvent.TELEGRAM_CHAT_UNREGISTERED)
                    .log("Telegram chat unregistered");
        }
    }
}
