package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import backend.academy.linktracker.scrapper.mapper.SubscriptionMapper;
import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.util.List;
import backend.academy.linktracker.scrapper.service.persistence.SubscriptionPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final TelegramChatRepository telegramChatRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LinkService linkService;
    private final SubscriptionPersistenceService subscriptionPersistenceService;
    private final SubscriptionMapper subscriptionMapper;

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

            // так как getOrCreateTrackedLink может ходить во внешний API,
            // то я не помечаю addSubscription аннотацией Transactional
            TrackedLink trackedLink = linkService.getOrCreateTrackedLink(request.link());

            var savedSubscription = subscriptionPersistenceService
                .createSubscription(trackedLink, telegramChat, request.tags());

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

            var removedSubscription = subscriptionPersistenceService
                .deleteSubscription(trackedLink, telegramChat);

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

    @Transactional(readOnly = true)
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

            List<Subscription> subscriptions = subscriptionRepository
                .findAllByTelegramChatId(telegramChat.id());
            List<LinkResponse> links = subscriptionMapper.toLinkResponses(subscriptions);

            log.atInfo()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_LIST_LOADED)
                    .addKeyValue("subscriptionsCount", links.size())
                    .log("Subscription list loaded");

            return new ListLinksResponse(links, links.size());
        } finally {
            MDC.clear();
        }
    }
}
