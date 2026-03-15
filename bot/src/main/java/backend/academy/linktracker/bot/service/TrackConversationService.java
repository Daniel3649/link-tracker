package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.conversation.TrackDialogState;
import backend.academy.linktracker.bot.conversation.handler.TrackStepHandler;
import backend.academy.linktracker.bot.conversation.handler.TrackStepHandlerRegistry;
import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.repository.TrackDialogStateRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackConversationService {
    private final TrackDialogStateRepository trackDialogStateRepository;
    private final TrackStepHandlerRegistry trackStepHandlerRegistry;

    public void start(long chatId) {
        trackDialogStateRepository.save(chatId, TrackDialogState.waitingLink());

        log.atInfo().addKeyValue("event", LogEvent.TRACK_DIALOG_STARTED).log("Track dialog started");
    }

    public void cancel(long chatId) {
        trackDialogStateRepository.deleteByChatId(chatId);

        log.atInfo().addKeyValue("event", LogEvent.TRACK_DIALOG_CANCELLED).log("Track dialog cancelled");
    }

    public boolean hasActiveSession(long chatId) {
        boolean hasActiveSession = trackDialogStateRepository.existsByChatId(chatId);

        log.atInfo()
                .addKeyValue("event", LogEvent.TRACK_DIALOG_SESSION_CHECKED)
                .addKeyValue("hasActiveSession", hasActiveSession)
                .log("Track dialog session checked");

        return hasActiveSession;
    }

    public boolean handleDialogMessage(long chatId, String rawText) {
        Objects.requireNonNull(rawText, "rawText cannot be null");

        return trackDialogStateRepository
                .findByChatId(chatId)
                .map(state -> {
                    TrackStepHandler handler = trackStepHandlerRegistry.getHandler(state);

                    handler.handle(chatId, rawText, state);

                    log.atInfo()
                            .addKeyValue("event", LogEvent.TRACK_DIALOG_MESSAGE_PROCESSED)
                            .addKeyValue("rawText", rawText)
                            .addKeyValue("state", state)
                            .addKeyValue("handler", handler.getClass().getSimpleName())
                            .log("Track dialog message processed");

                    return true;
                })
                .orElseGet(() -> {
                    log.atDebug()
                            .addKeyValue("event", LogEvent.TRACK_DIALOG_MESSAGE_IGNORED)
                            .addKeyValue("rawText", rawText)
                            .addKeyValue("reason", "no_active_session")
                            .log("Track dialog message ignored");

                    return false;
                });
    }
}
