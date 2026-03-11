package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.repository.TrackDialogStateRepository;
import backend.academy.linktracker.bot.track.TrackDialogState;
import backend.academy.linktracker.bot.track.handler.TrackStepHandler;
import backend.academy.linktracker.bot.track.handler.TrackStepHandlerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrackConversationService {
    private final TrackDialogStateRepository trackDialogStateRepository;
    private final TrackStepHandlerRegistry trackStepHandlerRegistry;

    public void start(long chatId) {
        trackDialogStateRepository.save(chatId, TrackDialogState.waitingLink());
    }

    public void cancel(long chatId) {
        trackDialogStateRepository.deleteByChatId(chatId);
    }

    public boolean hasActiveSession(long chatId) {
        return trackDialogStateRepository.existsByChatId(chatId);
    }

    public boolean handleDialogMessage(long chatId, String rawText) {
        Objects.requireNonNull(rawText);

        TrackDialogState state = trackDialogStateRepository
            .findByChatId(chatId).orElse(null);
        if (state == null) {
            return false;
        }

        TrackStepHandler handler = trackStepHandlerRegistry.getHandler(state);
        handler.handle(chatId, rawText, state);
        return true;
    }
}
