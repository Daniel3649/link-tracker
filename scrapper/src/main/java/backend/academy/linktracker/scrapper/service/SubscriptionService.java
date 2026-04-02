package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
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
import org.slf4j.MDC;
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
    private record SubscriptionRemovalResult(Subscription subscription, boolean trackedLinkDeleted) {}

    public LinkResponse addSubscription(long chatId, AddLinkRequest request) {
        try (var chatIdMdc = MDC.putCloseable("chatId", String.valueOf(chatId));
            var urlMdc = MDC.putCloseable("url", request.link().toString())) {
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
            Subscription savedSubscription = createSubscription(trackedLink, telegramChat, request.tags());

            log.atInfo()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_ADDED)
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .addKeyValue("subscriptionId", savedSubscription.getId())
                    .addKeyValue("tagsCount", request.tags() == null ? 0 : request.tags().size())
                    .log("Subscription added");

            return subscriptionMapper.toLinkResponse(savedSubscription);
        }
    }

    public LinkResponse removeSubscription(long chatId, RemoveLinkRequest request) {
        try (var chatIdMdc = MDC.putCloseable("chatId", String.valueOf(chatId));
            var urlMdc = MDC.putCloseable("url", request.link().toString())) {
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

            SubscriptionRemovalResult removalResult = deleteSubscription(trackedLink, telegramChat);

            log.atInfo()
                    .addKeyValue("event", LogEvent.SUBSCRIPTION_REMOVED)
                    .addKeyValue("trackedLinkId", trackedLink.getId())
                    .addKeyValue("subscriptionId", removalResult.subscription().getId())
                    .addKeyValue("trackedLinkDeleted", removalResult.trackedLinkDeleted())
                    .log("Subscription removed");

            return subscriptionMapper.toLinkResponse(removalResult.subscription());
        }
    }

    public ListLinksResponse getAllSubscriptions(long chatId) {
        try (var chatIdMdc = MDC.putCloseable("chatId", String.valueOf(chatId))) {
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
        }
    }

    private Subscription createSubscription(TrackedLink trackedLink, TelegramChat telegramChat, Set<String> tags) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        Object lock = linkOperationLocks.computeIfAbsent(mapKey, ignored -> new Object());

        try (var trackedLinkIdMdc = MDC.putCloseable("trackedLinkId", String.valueOf(trackedLink.getId()))) {
            synchronized (lock) {
                if (subscriptionRepository.existsByTrackedLinkAndTelegramChat(trackedLink, telegramChat)) {
                    log.atWarn()
                            .addKeyValue("event", LogEvent.SUBSCRIPTION_ADD_REJECTED)
                            .addKeyValue("reason", "subscription_already_exists")
                            .log("Subscription add rejected");

                    throw new SubscriptionAlreadyExistsException("Link is already tracked: " + trackedLink.getUrl());
                }

                Subscription savedSubscription =
                        subscriptionRepository.save(new Subscription(null, trackedLink, telegramChat));

                subscriptionTagRepository.addTags(savedSubscription, tags);

                return savedSubscription;
            }
        }
    }

    private SubscriptionRemovalResult deleteSubscription(TrackedLink trackedLink, TelegramChat telegramChat) {
        MapKey mapKey = new MapKey(trackedLink, telegramChat);
        Object lock = linkOperationLocks.computeIfAbsent(mapKey, ignored -> new Object());

        try (var trackedLinkIdMdc = MDC.putCloseable("trackedLinkId", String.valueOf(trackedLink.getId()))) {
            synchronized (lock) {
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

                boolean trackedLinkDeleted = false;
                if (!subscriptionRepository.existsByTrackedLink(trackedLink)) {
                    linkService.deleteTrackedLinkWithState(trackedLink);
                    trackedLinkDeleted = true;
                }

                return new SubscriptionRemovalResult(subscription, trackedLinkDeleted);
            }
        }
    }
}
