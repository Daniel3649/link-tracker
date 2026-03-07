package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryTelegramChatRepository implements TelegramChatRepository {
    private final ConcurrentMap<Long, TelegramChat> chats = new ConcurrentHashMap<>();

    @Override
    public Optional<TelegramChat> findByChatId(Long chatId) {
        return Optional.ofNullable(chats.get(chatId));
    }
}
