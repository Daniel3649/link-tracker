package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Optional<Subscription> saveIfAbsent(Subscription subscription);

    boolean existsByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat);

    Subscription save(Subscription subscription);

    Optional<Subscription> findByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat);

    Optional<Subscription> removeByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat);

    void deleteByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat);

    List<Subscription> findAllByTelegramChat(TelegramChat telegramChat);

    List<Subscription> findAllByTrackedLink(TrackedLink trackedLink);

    void clear();
}
