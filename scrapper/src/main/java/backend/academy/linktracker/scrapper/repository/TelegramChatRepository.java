package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import java.util.Optional;

public interface TelegramChatRepository {
    Optional<TelegramChat> findByChatId(Long chatId);

    TelegramChat save(TelegramChat telegramChat);

    boolean existsByChatId(Long chatId);

    void deleteByChatId(Long chatId);
}
