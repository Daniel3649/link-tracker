package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.contract.dto.request.AddTagRequest;
import backend.academy.linktracker.contract.dto.request.RemoveTagRequest;
import backend.academy.linktracker.contract.dto.request.UpdateTagRequest;
import backend.academy.linktracker.contract.dto.response.ListTagsResponse;
import backend.academy.linktracker.contract.dto.response.TagResponse;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.exception.tag.TagAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.tag.TagNotFoundException;
import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionTagService {
    private final TelegramChatRepository telegramChatRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTagRepository subscriptionTagRepository;
    private final LinkService linkService;

    public ListTagsResponse getTags(long chatId, URI link) {
        Subscription subscription = getSubscription(chatId, link);
        List<String> tags = subscriptionTagRepository.findAllBySubscription(subscription).stream()
                .sorted()
                .toList();
        return new ListTagsResponse(tags, tags.size());
    }

    @Transactional
    public TagResponse addTag(long chatId, AddTagRequest request) {
        Subscription subscription = getSubscription(chatId, request.link());
        String normalizedTag = request.tag().trim();

        if (!subscriptionTagRepository.addTag(subscription, normalizedTag)) {
            throw new TagAlreadyExistsException("Tag already exists: " + normalizedTag);
        }

        return new TagResponse(normalizedTag);
    }

    @Transactional
    public TagResponse updateTag(long chatId, UpdateTagRequest request) {
        Subscription subscription = getSubscription(chatId, request.link());
        String currentTag = request.currentTag().trim();
        String newTag = request.newTag().trim();

        if (!subscriptionTagRepository.existsBySubscriptionAndTag(subscription, currentTag)) {
            throw new TagNotFoundException("Tag not found: " + currentTag);
        }

        if (!currentTag.equals(newTag) && subscriptionTagRepository.existsBySubscriptionAndTag(subscription, newTag)) {
            throw new TagAlreadyExistsException("Tag already exists: " + newTag);
        }

        if (!currentTag.equals(newTag) && !subscriptionTagRepository.updateTag(subscription, currentTag, newTag)) {
            throw new TagNotFoundException("Tag not found: " + currentTag);
        }

        return new TagResponse(newTag);
    }

    @Transactional
    public TagResponse removeTag(long chatId, RemoveTagRequest request) {
        Subscription subscription = getSubscription(chatId, request.link());
        String normalizedTag = request.tag().trim();

        if (!subscriptionTagRepository.deleteTag(subscription, normalizedTag)) {
            throw new TagNotFoundException("Tag not found: " + normalizedTag);
        }

        return new TagResponse(normalizedTag);
    }

    private Subscription getSubscription(long chatId, URI link) {
        TelegramChat telegramChat = telegramChatRepository
                .findByChatId(chatId)
                .orElseThrow(() -> new TelegramChatNotFoundException("Chat not found. Id: " + chatId));

        TrackedLink trackedLink = linkService
                .findTrackedLink(link)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found for link: " + link));

        return subscriptionRepository
                .findByTrackedLinkAndTelegramChat(trackedLink, telegramChat)
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found for link: " + link));
    }
}
