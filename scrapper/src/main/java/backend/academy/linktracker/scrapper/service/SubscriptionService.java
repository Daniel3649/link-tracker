package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.mapper.SubscriptionMapper;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final TelegramChatRepository telegramChatRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LinkService linkService;
    private final SubscriptionPersistenceService subscriptionPersistenceService;
    private final SubscriptionMapper subscriptionMapper;

    private final ConcurrentMap<MapKey, Object> linkOperationLocks = new ConcurrentHashMap<>();

    private record MapKey(TrackedLink trackedLink, TelegramChat telegramChat) {}

    public LinkResponse addSubscription(long chatId, AddLinkRequest request) {
        try {
            MDC.put("chatId", String.valueOf(chatId));
            MDC.put("url", request.link().toString());
            MDC.put(
                    "tagsCount",
                    request.tags() == null ? "0" : String.valueOf(request.tags().size()));

            log.atInfo().addKeyValue("event", LogEvent.SUBSCRIPTION_ADD_STARTED).log("Subscription add started");
            TelegramChat telegramChat = telegramChatRepository
                    .findByChatId(chatId)
                    .orElseThrow(() -> {
                        log.atWarn()
                                .addKeyValue("event", LogEvent.SUBSCRIPTION_ADD_FAILED)
                                .addKeyValue("reason", "telegram_chat_not_found")
                                .log("Subscription add failed");

                        return new TelegramChatNotFoundException("Chat not found. Id: " + chatId);
                    });

            TrackedLink trackedLink = linkService.getOrCreateTrackedLink(request.link());
            var savedSubscription = createSubscription(trackedLink, telegramChat, request.tags());

            log.atInfo()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_ADDED)
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .addKeyValue("subscriptionId", savedSubscription.getId())
                    .log("Subscription added");

            return subscriptionMapper.toLinkResponse(savedSubscription);
        } finally {
            MDC.clear();
        }
    }

    public LinkResponse removeSubscription(long chatId, RemoveLinkRequest request) {
        try {
            MDC.put("chatId", String.valueOf(chatId));
            MDC.put("url", request.link().toString());

            log.atInfo()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_REMOVE_STARTED)
                    .log("Subscription remove started");

            TelegramChat telegramChat = telegramChatRepository
                    .findByChatId(chatId)
                    .orElseThrow(() -> {
                        log.atWarn()
                                .addKeyValue("event", LogEvent.SUBSCRIPTION_REMOVE_CHAT_NOT_FOUND)
                                .log("Cannot remove subscription because chat was not found");

                        return new TelegramChatNotFoundException("Chat not found. Id: " + chatId);
                    });

            TrackedLink trackedLink = linkService
                    .findTrackedLink(request.link())
                    .orElseThrow(() -> {
                        log.atWarn()
                                .addKeyValue("event", LogEvent.SUBSCRIPTION_REMOVE_TRACKED_LINK_NOT_FOUND)
                                .log("Cannot remove subscription because tracked link was not found");

                        return new SubscriptionNotFoundException("Subscription not found for link: " + request.link());
                    });

            var removedSubscription = deleteSubscription(trackedLink, telegramChat);

            log.atInfo()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_REMOVED)
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .addKeyValue("subscriptionId", removedSubscription.getId())
                    .log("Subscription removed");

            return subscriptionMapper.toLinkResponse(removedSubscription);
        } finally {
            MDC.clear();
        }
    }

    public ListLinksResponse getAllSubscriptions(long chatId) {
        try {
            MDC.put("chatId", String.valueOf(chatId));

            log.atDebug()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_LIST_REQUESTED)
                    .log("Subscription list requested");

            TelegramChat telegramChat = telegramChatRepository
                    .findByChatId(chatId)
                    .orElseThrow(() -> {
                        log.atWarn()
                                .addKeyValue("event", LogEvent.SUBSCRIPTION_LIST_FAILED)
                                .addKeyValue("reason", "telegram_chat_not_found")
                                .log("Subscription list failed");

                        return new TelegramChatNotFoundException("Chat not found. Id: " + chatId);
                    });

            List<LinkResponse> links =
                    subscriptionMapper.toLinkResponses(subscriptionRepository.findAllByTelegramChat(telegramChat));

            log.atInfo()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_LIST_LOADED)
                    .addKeyValue("subscriptionsCount", links.size())
                    .log("Subscription list loaded");

            return new ListLinksResponse(links, links.size());
        } finally {
            MDC.clear();
        }
    }

    private backend.academy.linktracker.scrapper.models.subscription.Subscription createSubscription(
            TrackedLink trackedLink, TelegramChat telegramChat, java.util.Set<String> tags) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        Object lock = linkOperationLocks.computeIfAbsent(mapKey, ignored -> new Object());

        try {
            MDC.put("chatId", String.valueOf(telegramChat.id()));
            MDC.put("url", trackedLink.getUrl());
            MDC.put("trackedLinkId", String.valueOf(trackedLink.getId()));

            synchronized (lock) {
                return subscriptionPersistenceService.createSubscription(trackedLink, telegramChat, tags);
            }
        } finally {
            MDC.clear();
        }
    }

    private backend.academy.linktracker.scrapper.models.subscription.Subscription deleteSubscription(
            TrackedLink trackedLink, TelegramChat telegramChat) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        Object lock = linkOperationLocks.computeIfAbsent(mapKey, ignored -> new Object());

        try {
            MDC.put("chatId", String.valueOf(telegramChat.id()));
            MDC.put("url", trackedLink.getUrl());
            MDC.put("trackedLinkId", String.valueOf(trackedLink.getId()));

            synchronized (lock) {
                return subscriptionPersistenceService.deleteSubscription(trackedLink, telegramChat);
            }
        } finally {
            MDC.clear();
        }
    }
}
