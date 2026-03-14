package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.mapper.SubscriptionMapper;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final TelegramChatRepository telegramChatRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTagRepository subscriptionTagRepository;
    private final LinkService linkService;
    private final SubscriptionMapper subscriptionMapper;

    private final ConcurrentMap<MapKey, Object> linkOperationLocks = new ConcurrentHashMap<>();

    private record MapKey(TrackedLink trackedLink, TelegramChat telegramChat) {}

    public LinkResponse addSubscription(long chatId, AddLinkRequest request) {
        log.atInfo()
            .addKeyValue("event", "subscription_add_started")
            .addKeyValue("chatId", chatId)
            .addKeyValue("url", request.link())
            .addKeyValue("tagsCount", request.tags() == null ? 0 : request.tags().size())
            .log("Subscription add started");

        TelegramChat telegramChat = telegramChatRepository
            .findByChatId(chatId)
            .orElseThrow(() -> {
                log.atWarn()
                    .addKeyValue("event", "subscription_add_failed")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", request.link())
                    .addKeyValue("reason", "telegram_chat_not_found")
                    .log("Subscription add failed");

                return new TelegramChatNotFoundException("Chat not found. Id: " + chatId);
            });

        TrackedLink trackedLink = linkService.getOrCreateTrackedLink(request.link());
        Subscription savedSubscription = createSubscription(trackedLink, telegramChat, request.tags());

        log.atInfo()
            .addKeyValue("event", "subscription_added")
            .addKeyValue("chatId", chatId)
            .addKeyValue("trackedLinkId", trackedLink.getId())
            .addKeyValue("url", trackedLink.getUrl())
            .addKeyValue("tagsCount", request.tags() == null ? 0 : request.tags().size())
            .addKeyValue("subscriptionId", savedSubscription.getId())
            .log("Subscription added");

        return subscriptionMapper.toLinkResponse(savedSubscription);
    }

    public LinkResponse removeSubscription(long chatId, RemoveLinkRequest request) {
        log.atInfo()
            .addKeyValue("event", "subscription_remove_started")
            .addKeyValue("chatId", chatId)
            .addKeyValue("url", request.link())
            .log("Subscription remove started");

        TelegramChat telegramChat = telegramChatRepository
            .findByChatId(chatId)
            .orElseThrow(() -> {
                log.atWarn()
                    .addKeyValue("event", "subscription_remove_chat_not_found")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", request.link())
                    .log("Cannot remove subscription because chat was not found");


                return new TelegramChatNotFoundException("Chat not found. Id: " + chatId);
            });

        TrackedLink trackedLink = linkService
            .findTrackedLink(request.link())
            .orElseThrow(() -> {
                log.atWarn()
                    .addKeyValue("event", "subscription_remove_tracked_link_not_found")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("url", request.link())
                    .log("Cannot remove subscription because tracked link was not found");

                return new SubscriptionNotFoundException("Subscription not found for link: " + request.link());
            });

        Subscription removedSubscription = deleteSubscription(trackedLink, telegramChat);

        log.atInfo()
            .addKeyValue("event", "subscription_removed")
            .addKeyValue("chatId", chatId)
            .addKeyValue("trackedLinkId", trackedLink.getId())
            .addKeyValue("url", trackedLink.getUrl())
            .addKeyValue("subscriptionId", removedSubscription.getId())
            .log("Subscription removed");

        return subscriptionMapper.toLinkResponse(removedSubscription);
    }

    public ListLinksResponse getAllSubscriptions(long chatId) {
        log.atDebug()
            .addKeyValue("event", "subscription_list_requested")
            .addKeyValue("chatId", chatId)
            .log("Subscription list requested");

        TelegramChat telegramChat = telegramChatRepository
            .findByChatId(chatId)
            .orElseThrow(() -> {
                log.atWarn()
                    .addKeyValue("event", "subscription_list_failed")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("reason", "telegram_chat_not_found")
                    .log("Subscription list failed");

                return new TelegramChatNotFoundException("Chat not found. Id: " + chatId);
            });

        List<LinkResponse> links =
            subscriptionMapper.toLinkResponses(subscriptionRepository.findAllByTelegramChat(telegramChat));

        log.atInfo()
            .addKeyValue("event", "subscription_list_loaded")
            .addKeyValue("chatId", chatId)
            .addKeyValue("subscriptionsCount", links.size())
            .log("Subscription list loaded");

        return new ListLinksResponse(links, links.size());
    }

    private Subscription createSubscription(TrackedLink trackedLink, TelegramChat telegramChat, Set<String> tags) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        Object lock = linkOperationLocks.computeIfAbsent(mapKey, ignored -> new Object());

        synchronized (lock) {
            if (subscriptionRepository.existsByTrackedLinkAndTelegramChat(trackedLink, telegramChat)) {
                log.atWarn()
                    .addKeyValue("event", "subscription_add_rejected")
                    .addKeyValue("chatId", telegramChat.getId())
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .addKeyValue("url", trackedLink.getUrl())
                    .addKeyValue("reason", "subscription_already_exists")
                    .log("Subscription add rejected");

                throw new SubscriptionAlreadyExistsException("Link is already tracked: " + trackedLink.getUrl());
            }

            Subscription savedSubscription =
                subscriptionRepository.save(new Subscription(null, trackedLink, telegramChat));

            subscriptionTagRepository.addTags(savedSubscription, tags);

            log.atInfo()
                .addKeyValue("event", "subscription_persisted")
                .addKeyValue("chatId", telegramChat.getId())
                .addKeyValue("trackedLinkId", trackedLink.getId())
                .addKeyValue("url", trackedLink.getUrl())
                .addKeyValue("subscriptionId", savedSubscription.getId())
                .addKeyValue("tagsCount", tags == null ? 0 : tags.size())
                .log("Subscription persisted");

            return savedSubscription;
        }
    }

    private Subscription deleteSubscription(TrackedLink trackedLink, TelegramChat telegramChat) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        Object lock = linkOperationLocks.computeIfAbsent(mapKey, ignored -> new Object());

        synchronized (lock) {
            Subscription subscription = subscriptionRepository
                .findByTrackedLinkAndTelegramChat(trackedLink, telegramChat)
                .orElseThrow(() -> {
                    log.atWarn()
                        .addKeyValue("event", "subscription_remove_rejected")
                        .addKeyValue("chatId", telegramChat.getId())
                        .addKeyValue("trackedLinkId", trackedLink.getId())
                        .addKeyValue("url", trackedLink.getUrl())
                        .addKeyValue("reason", "subscription_not_found")
                        .log("Subscription remove rejected");

                    return new SubscriptionNotFoundException(
                        "Subscription not found for link: " + trackedLink.getUrl()
                    );
                });

            subscriptionTagRepository.deleteAllBySubscription(subscription);
            subscriptionRepository.deleteByTrackedLinkAndTelegramChat(trackedLink, telegramChat);

            boolean trackedLinkHasSubscribers = subscriptionRepository.existsByTrackedLink(trackedLink);
            if (!trackedLinkHasSubscribers) {
                linkService.deleteTrackedLinkWithState(trackedLink);

                log.atInfo()
                    .addKeyValue("event", "orphan_tracked_link_deleted")
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .addKeyValue("url", trackedLink.getUrl())
                    .log("Orphan tracked link deleted");
            }

            log.atInfo()
                .addKeyValue("event", "subscription_deleted")
                .addKeyValue("chatId", telegramChat.getId())
                .addKeyValue("trackedLinkId", trackedLink.getId())
                .addKeyValue("url", trackedLink.getUrl())
                .addKeyValue("subscriptionId", subscription.getId())
                .addKeyValue("trackedLinkHasSubscribers", trackedLinkHasSubscribers)
                .log("Subscription deleted");

            return subscription;
        }
    }
}
