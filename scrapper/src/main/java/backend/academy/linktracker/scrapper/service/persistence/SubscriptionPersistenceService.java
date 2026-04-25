package backend.academy.linktracker.scrapper.service.persistence;

import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.service.LinkService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPersistenceService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTagRepository subscriptionTagRepository;
    private final LinkService linkService;

    @Transactional
    public Subscription createSubscription(TrackedLink trackedLink, TelegramChat telegramChat, Set<String> tags) {
        Subscription savedSubscription = subscriptionRepository
                .saveIfAbsent(new Subscription(null, trackedLink, telegramChat))
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event", LogEvent.SUBSCRIPTION_ADD_REJECTED)
                            .addKeyValue("reason", "subscription_already_exists")
                            .log("Subscription add rejected");

                    return new SubscriptionAlreadyExistsException("Link is already tracked: " + trackedLink.getUrl());
                });

        subscriptionTagRepository.addTags(savedSubscription, tags);

        log.atInfo()
                .addKeyValue("event", LogEvent.SUBSCRIPTION_PERSISTED)
                .addKeyValue("subscriptionId", savedSubscription.getId())
                .addKeyValue("tagsCount", tags == null ? 0 : tags.size())
                .log("Subscription persisted");

        return savedSubscription;
    }

    @Transactional
    public Subscription deleteSubscription(TrackedLink trackedLink, TelegramChat telegramChat) {
        Subscription subscription = subscriptionRepository
                .findByTrackedLinkAndTelegramChat(trackedLink, telegramChat)
                .orElseThrow(() -> {
                    log.atWarn()
                            .addKeyValue("event", LogEvent.SUBSCRIPTION_REMOVE_REJECTED)
                            .addKeyValue("reason", "subscription_not_found")
                            .log("Subscription remove rejected");

                    return new SubscriptionNotFoundException(
                            "Subscription not found for link: " + trackedLink.getUrl());
                });

        subscriptionTagRepository.deleteAllBySubscription(subscription);
        subscriptionRepository.deleteByTrackedLinkAndTelegramChat(trackedLink, telegramChat);

        boolean trackedLinkHasSubscribers = subscriptionRepository.existsByTrackedLink(trackedLink);
        if (!trackedLinkHasSubscribers) {
            linkService.deleteTrackedLinkWithState(trackedLink);

            log.atInfo()
                    .addKeyValue("event", LogEvent.ORPHAN_TRACKED_LINK_DELETED)
                    .log("Orphan tracked link deleted");
        }

        log.atInfo()
                .addKeyValue("event", LogEvent.SUBSCRIPTION_DELETED)
                .addKeyValue("subscriptionId", subscription.getId())
                .addKeyValue("trackedLinkHasSubscribers", trackedLinkHasSubscribers)
                .log("Subscription deleted");

        return subscription;
    }
}
