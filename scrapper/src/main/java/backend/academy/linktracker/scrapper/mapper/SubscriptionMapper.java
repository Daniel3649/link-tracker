package backend.academy.linktracker.scrapper.mapper;

import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionMapper {
    private final SubscriptionTagRepository subscriptionTagRepository;

    public LinkResponse toLinkResponse(Subscription subscription) {
        TrackedLink trackedLink = subscription.getTrackedLink();

        List<String> tags = subscriptionTagRepository.findAllBySubscription(subscription).stream()
                .sorted()
                .toList();

        return new LinkResponse(trackedLink.getId(), URI.create(trackedLink.getUrl()), tags, List.of());
    }

    public List<LinkResponse> toLinkResponses(List<Subscription> subscriptions) {
        return subscriptions.stream().map(this::toLinkResponse).toList();
    }
}
