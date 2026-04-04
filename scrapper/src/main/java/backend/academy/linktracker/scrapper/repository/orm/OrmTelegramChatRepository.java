package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.TelegramChatEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TelegramChatJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class OrmTelegramChatRepository implements TelegramChatRepository {
    private final TelegramChatJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public Optional<TelegramChat> findByChatId(Long chatId) {
        return repository.findById(chatId).map(ignored -> new TelegramChat(chatId));
    }

    @Override
    @Transactional
    public boolean saveIfAbsent(TelegramChat telegramChat) {
        try {
            repository.saveAndFlush(new TelegramChatEntity(telegramChat.id()));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public TelegramChat save(TelegramChat telegramChat) {
        TelegramChatEntity entity = repository.save(new TelegramChatEntity(telegramChat.id()));
        return new TelegramChat(entity.getId());
    }

    @Override
    public Optional<TelegramChat> removeByChatId(Long chatId) {
        Optional<TelegramChat> telegramChat = findByChatId(chatId);
        if (telegramChat.isEmpty()) {
            return Optional.empty();
        }

        repository.deleteById(chatId);
        return telegramChat;
    }

    @Override
    public void clear() {
        entityManager.createNativeQuery("truncate table telegram_chat cascade").executeUpdate();
    }
}
