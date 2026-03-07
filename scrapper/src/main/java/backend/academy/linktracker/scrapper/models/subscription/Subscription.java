package backend.academy.linktracker.scrapper.models.subscription;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Objects;


@RequiredArgsConstructor
@Getter
public class Subscription {
    private final Long id;

    private final TrackedLink trackedLink;
    private final TelegramChat telegramChat;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription subscription = (Subscription) o;
        return trackedLink.equals(subscription.trackedLink) &&
            telegramChat.getId().equals(subscription.telegramChat.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(trackedLink, telegramChat.getId());
    }
}
