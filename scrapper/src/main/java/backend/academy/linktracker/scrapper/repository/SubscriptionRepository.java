package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Optional<Subscription> saveIfAbsent(Subscription subscription);

    boolean existsByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat);

    Subscription save(Subscription subscription);

    boolean existsByTrackedLink(TrackedLink trackedLink);

    Optional<Subscription> findByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat);

    void deleteByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat);

    List<Subscription> findAllByTelegramChatId(Long id);

    List<Subscription> findAllByTrackedLink(TrackedLink trackedLink);

    void clear();
}
