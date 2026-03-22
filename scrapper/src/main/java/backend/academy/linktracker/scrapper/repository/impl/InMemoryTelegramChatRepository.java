package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public class InMemoryTelegramChatRepository implements TelegramChatRepository {
    private final ConcurrentMap<Long, TelegramChat> chats = new ConcurrentHashMap<>();

    @Override
    public Optional<TelegramChat> findByChatId(Long chatId) {
        return Optional.ofNullable(chats.get(chatId));
    }

    @Override
    public boolean saveIfAbsent(TelegramChat telegramChat) {
        return chats.putIfAbsent(telegramChat.id(), telegramChat) == null;
    }

    @Override
    public TelegramChat save(TelegramChat telegramChat) {
        chats.put(telegramChat.id(), telegramChat);
        return telegramChat;
    }

    @Override
    public boolean existsByChatId(Long chatId) {
        return chats.containsKey(chatId);
    }

    @Override
    public void deleteByChatId(Long chatId) {
        chats.remove(chatId);
    }

    @Override
    public void clear() {
        chats.clear();
    }
}
