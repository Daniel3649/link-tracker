package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.TelegramChatEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TelegramChatJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;

@RequiredArgsConstructor
public class OrmTelegramChatRepository implements TelegramChatRepository {
    private final TelegramChatJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public Optional<TelegramChat> findByChatId(Long chatId) {
        return repository.findById(chatId).map(entity -> new TelegramChat(entity.getId()));
    }

    @Override
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
        TelegramChatEntity entity = repository.saveAndFlush(new TelegramChatEntity(telegramChat.id()));
        return new TelegramChat(entity.getId());
    }

    @Override
    public boolean existsByChatId(Long chatId) {
        return repository.existsById(chatId);
    }

    @Override
    public void deleteByChatId(Long chatId) {
        repository.deleteById(chatId);
    }

    @Override
    public void clear() {
        entityManager.createNativeQuery("truncate table telegram_chat cascade").executeUpdate();
    }
}
