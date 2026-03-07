package backend.academy.linktracker.scrapper.models.subscription;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SubscriptionTag {
    private final Subscription subscription;
    private final String tag;
}
