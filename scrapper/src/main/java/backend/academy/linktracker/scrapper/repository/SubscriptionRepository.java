package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;

public interface SubscriptionRepository {
    boolean existsByResourceKeyAndChatId(TrackedLink trackedLink, TelegramChat telegramChat);
    Subscription save(Subscription subscription);
}
