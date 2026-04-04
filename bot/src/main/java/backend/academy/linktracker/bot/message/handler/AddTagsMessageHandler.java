package backend.academy.linktracker.bot.message.handler;

import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.bot.service.TrackConversationService;
import backend.academy.linktracker.bot.tracksession.DialogueState;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddTagsMessageHandler implements MessageHandler {
    private final TrackConversationService trackConversationService;
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public boolean supports(DialogueState state) {
        return state == DialogueState.WAITING_TAGS;
    }

    @Override
    public void handle(long chatId, String rawText) {
        Set<String> tags = parseTags(rawText);
        trackConversationService.acceptTags(chatId, tags);
        telegramSender.sendPlain(chatId, messageService.get("link.add.success"));
    }

    private Set<String> parseTags(String rawText) {
        String normalized = rawText == null ? "" : rawText.strip();

        if (normalized.isBlank() || normalized.equals("-")) {
            return Set.of();
        }

        LinkedHashSet<String> result = Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return Collections.unmodifiableSet(result);
    }
}
