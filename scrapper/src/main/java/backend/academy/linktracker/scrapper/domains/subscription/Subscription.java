package backend.academy.linktracker.scrapper.domains.subscription;

import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
