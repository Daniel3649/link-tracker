package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TelegramChatEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.SubscriptionJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class OrmSubscriptionRepository implements SubscriptionRepository {
    private final SubscriptionJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public Optional<Subscription> saveIfAbsent(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setTrackedLink(entityManager.getReference(
                TrackedLinkEntity.class, subscription.getTrackedLink().getId()));
        entity.setTelegramChat(entityManager.getReference(
                TelegramChatEntity.class, subscription.getTelegramChat().id()));

        try {
            repository.saveAndFlush(entity);
            return Optional.of(toDomain(entity));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        return repository.existsByTrackedLinkIdAndTelegramChatChatId(trackedLink.getId(), telegramChat.id());
    }

    @Override
    public Subscription save(Subscription subscription) {
        if (subscription.getId() != null) {
            SubscriptionEntity entity = repository
                    .findById(subscription.getId())
                    .orElseThrow(() -> new SubscriptionNotFoundException(
                            "Subscription not found with id: " + subscription.getId()));

            entity.setTrackedLink(entityManager.getReference(
                    TrackedLinkEntity.class, subscription.getTrackedLink().getId()));
            entity.setTelegramChat(entityManager.getReference(
                    TelegramChatEntity.class, subscription.getTelegramChat().id()));

            return toDomain(entity);
        }

        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setTrackedLink(entityManager.getReference(
                TrackedLinkEntity.class, subscription.getTrackedLink().getId()));
        entity.setTelegramChat(entityManager.getReference(
                TelegramChatEntity.class, subscription.getTelegramChat().id()));
        repository.save(entity);
        return toDomain(entity);
    }

    @Override
    public boolean existsByTrackedLink(TrackedLink trackedLink) {
        return repository.existsByTrackedLinkId(trackedLink.getId());
    }

    @Override
    public Optional<Subscription> findByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        return repository
                .findByTrackedLinkIdAndTelegramChatChatId(trackedLink.getId(), telegramChat.id())
                .map(this::toDomain);
    }

    @Override
    public void deleteByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        repository.deleteByTrackedLinkIdAndTelegramChatChatId(trackedLink.getId(), telegramChat.id());
    }

    @Override
    public List<Subscription> findAllByTelegramChatId(Long chatId) {
        return repository.findAllByTelegramChatChatIdOrderByIdAsc(chatId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Subscription> findAllByTrackedLink(TrackedLink trackedLink) {
        return repository.findAllByTrackedLinkIdOrderByIdAsc(trackedLink.getId()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void clear() {
        entityManager.createNativeQuery("truncate table subscription cascade").executeUpdate();
    }

    private Subscription toDomain(SubscriptionEntity entity) {
        TrackedLink trackedLink = OrmTrackedLinkSupport.toDomain(entity.getTrackedLink());
        TelegramChat telegramChat = new TelegramChat(entity.getTelegramChat().getChatId());
        return new Subscription(entity.getId(), trackedLink, telegramChat);
    }
}
