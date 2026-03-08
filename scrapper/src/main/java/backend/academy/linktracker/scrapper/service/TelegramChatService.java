package backend.academy.linktracker.scrapper.service;


import backend.academy.linktracker.scrapper.exception.chat.TelegramChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class TelegramChatService {
    private final TelegramChatRepository telegramChatRepository;

    private final ConcurrentMap<Long, Object> chatRegistrationLocks = new ConcurrentHashMap<>();

    public void registerChat(long chatId) {
        Object lock = chatRegistrationLocks.computeIfAbsent(chatId, ignored -> new Object());

        synchronized (lock) {
            if (telegramChatRepository.existsByChatId(chatId)) {
                throw new TelegramChatAlreadyExistsException(
                    "Telegram chat already exists. Id: " + chatId
                );
            }

            telegramChatRepository.save(new TelegramChat(chatId));
        }
    }

    public void unregisterChat(long chatId) {
        Object lock = chatRegistrationLocks.computeIfAbsent(chatId, ignored -> new Object());

        synchronized (lock) {
            if (!telegramChatRepository.existsByChatId(chatId)) {
                throw new TelegramChatNotFoundException(
                    "Telegram chat not found. Id: " + chatId
                );
            }

            telegramChatRepository.deleteByChatId(chatId);
        }
    }
}
