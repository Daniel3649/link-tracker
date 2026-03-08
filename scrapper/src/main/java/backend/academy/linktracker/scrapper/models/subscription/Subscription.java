package backend.academy.linktracker.scrapper.models.subscription;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Objects;


@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Subscription {
    private final Long id;

    @EqualsAndHashCode.Include
    private final TrackedLink trackedLink;

    @EqualsAndHashCode.Include
    private final TelegramChat telegramChat;
}
