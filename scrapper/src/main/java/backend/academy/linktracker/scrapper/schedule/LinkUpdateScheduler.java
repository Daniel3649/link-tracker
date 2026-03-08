package backend.academy.linktracker.scrapper.schedule;

import backend.academy.linktracker.scrapper.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.handlers.LinkHandler;
import backend.academy.linktracker.scrapper.handlers.common.LinkChange;
import backend.academy.linktracker.scrapper.handlers.registry.LinkHandlerRegistry;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TrackedLinkRepository;
import backend.academy.linktracker.scrapper.sender.LinkUpdateSender;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkUpdateScheduler {
    private final TrackedLinkRepository trackedLinkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final LinkHandlerRegistry linkHandlerRegistry;
    private final LinkUpdateSender linkUpdateSender;

    @Scheduled(fixedDelayString = "${app.scheduler.link-check-delay-ms}")
    public void checkUpdates() {
        for (TrackedLink trackedLink : trackedLinkRepository.findAll()) {
            try {
                URI uri = URI.create(trackedLink.getUrl());
                LinkHandler handler = linkHandlerRegistry.getHandler(uri);

                handler.checkForUpdate(trackedLink).ifPresent(change -> sendUpdate(trackedLink, change));
            } catch (RepositoryPollingException e) {
                log.warn("Repository polling failed for link {}", trackedLink.getUrl(), e);
            } catch (Exception e) {
                log.error("Unexpected error while checking link {}", trackedLink.getUrl(), e);
            }
        }
    }

    private void sendUpdate(TrackedLink trackedLink, LinkChange change) {
        List<Long> tgChatIds = subscriptionRepository.findAllByTrackedLink(trackedLink).stream()
                .map(subscription -> subscription.getTelegramChat().getId())
                .toList();

        if (tgChatIds.isEmpty()) {
            return;
        }

        LinkUpdate update =
                new LinkUpdate(trackedLink.getId(), URI.create(trackedLink.getUrl()), change.description(), tgChatIds);

        linkUpdateSender.send(update);
    }
}
