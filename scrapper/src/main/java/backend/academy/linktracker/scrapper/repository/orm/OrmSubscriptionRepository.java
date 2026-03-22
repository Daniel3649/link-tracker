package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TelegramChatEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.SubscriptionJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import java.util.List;


@RequiredArgsConstructor
public class OrmSubscriptionRepository implements SubscriptionRepository {
    private final SubscriptionJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public boolean existsByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        return repository.existsByTrackedLink_IdAndTelegramChat_Id(trackedLink.getId(), telegramChat.id());
    }

    @Override
    public Subscription save(Subscription subscription) {
        if (subscription.getId() != null) {
            SubscriptionEntity entity = repository.findById(subscription.getId()).orElseThrow();
            entity.setTrackedLink(entityManager.getReference(TrackedLinkEntity.class, subscription.getTrackedLink().getId()));
            entity.setTelegramChat(entityManager.getReference(TelegramChatEntity.class, subscription.getTelegramChat().id()));
            return toDomain(repository.saveAndFlush(entity));
        }

        return repository
                .findByTrackedLink_IdAndTelegramChat_Id(
                        subscription.getTrackedLink().getId(), subscription.getTelegramChat().id())
                .map(this::toDomain)
                .orElseGet(() -> {
                    SubscriptionEntity entity = new SubscriptionEntity();
                    entity.setTrackedLink(entityManager.getReference(
                            TrackedLinkEntity.class, subscription.getTrackedLink().getId()));
                    entity.setTelegramChat(entityManager.getReference(
                            TelegramChatEntity.class, subscription.getTelegramChat().id()));
                    return toDomain(repository.saveAndFlush(entity));
                });
    }

    @Override
    public boolean existsByTrackedLink(TrackedLink trackedLink) {
        return repository.existsByTrackedLink_Id(trackedLink.getId());
    }

    @Override
    public java.util.Optional<Subscription> findByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        return repository.findByTrackedLink_IdAndTelegramChat_Id(trackedLink.getId(), telegramChat.id()).map(this::toDomain);
    }

    @Override
    public void deleteByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        repository.deleteByTrackedLink_IdAndTelegramChat_Id(trackedLink.getId(), telegramChat.id());
    }

    @Override
    public List<Subscription> findAllByTelegramChat(TelegramChat telegramChat) {
        return repository.findAllByTelegramChat_IdOrderByIdAsc(telegramChat.id()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Subscription> findAllByTrackedLink(TrackedLink trackedLink) {
        return repository.findAllByTrackedLink_IdOrderByIdAsc(trackedLink.getId()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void clear() {
        entityManager.createNativeQuery("truncate table subscription cascade").executeUpdate();
    }

    private Subscription toDomain(SubscriptionEntity entity) {
        TrackedLink trackedLink = OrmTrackedLinkSupport.toDomain(entity.getTrackedLink());
        TelegramChat telegramChat = new TelegramChat(entity.getTelegramChat().getId());
        return new Subscription(entity.getId(), trackedLink, telegramChat);
    }
}
