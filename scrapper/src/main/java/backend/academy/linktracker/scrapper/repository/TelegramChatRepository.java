package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import java.util.Optional;

public interface TelegramChatRepository {
    Optional<TelegramChat> findByChatId(Long chatId);

    boolean saveIfAbsent(TelegramChat telegramChat);

    TelegramChat save(TelegramChat telegramChat);

    Optional<TelegramChat> removeByChatId(Long chatId);

    void clear();
}
