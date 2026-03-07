package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTagRepository subscriptionTagRepository;

    public void createSubscription(TrackedLink trackedLink, TelegramChat telegramChat, Set<String> tags) {
        if (subscriptionRepository.existsByResourceKeyAndChatId(trackedLink, telegramChat)) {
            throw new SubscriptionAlreadyExistsException("Link is already tracked: " + trackedLink.getUrl());
        }

        Subscription subscription = new Subscription(null, trackedLink, telegramChat);
        subscriptionRepository.save(subscription);
        subscriptionTagRepository.addTags(subscription, tags);
    }
}
