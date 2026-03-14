package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.repository.TrackDialogStateRepository;
import backend.academy.linktracker.bot.conversation.TrackDialogState;
import backend.academy.linktracker.bot.conversation.handler.TrackStepHandler;
import backend.academy.linktracker.bot.conversation.handler.TrackStepHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackConversationService {
    private final TrackDialogStateRepository trackDialogStateRepository;
    private final TrackStepHandlerRegistry trackStepHandlerRegistry;

    public void start(long chatId) {
        trackDialogStateRepository.save(chatId, TrackDialogState.waitingLink());

        log.atInfo()
            .addKeyValue("event", "track_dialog_started")
            .addKeyValue("chatId", chatId)
            .log("Track dialog started");
    }

    public void cancel(long chatId) {
        trackDialogStateRepository.deleteByChatId(chatId);

        log.atInfo()
            .addKeyValue("event", "track_dialog_cancelled")
            .addKeyValue("chatId", chatId)
            .log("Track dialog cancelled");
    }

    public boolean hasActiveSession(long chatId) {
        boolean hasActiveSession = trackDialogStateRepository.existsByChatId(chatId);

        log.atInfo()
            .addKeyValue("event", "track_dialog_session_checked")
            .addKeyValue("chatId", chatId)
            .addKeyValue("hasActiveSession", hasActiveSession)
            .log("Track dialog session checked");

        return hasActiveSession;
    }

    public boolean handleDialogMessage(long chatId, String rawText) {
        Objects.requireNonNull(rawText, "rawText cannot be null");

        return trackDialogStateRepository.findByChatId(chatId)
            .map(state -> {
                TrackStepHandler handler = trackStepHandlerRegistry.getHandler(state);

                log.atInfo()
                    .addKeyValue("event", "track_dialog_message_received")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("rawText", rawText)
                    .addKeyValue("state", state)
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Track dialog message received");

                handler.handle(chatId, rawText, state);

                log.atInfo()
                    .addKeyValue("event", "track_dialog_message_processed")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("rawText", rawText)
                    .addKeyValue("state", state)
                    .addKeyValue("handler", handler.getClass().getSimpleName())
                    .log("Track dialog message processed");

                return true;
            })
            .orElseGet(() -> {
                log.atDebug()
                    .addKeyValue("event", "track_dialog_message_ignored")
                    .addKeyValue("chatId", chatId)
                    .addKeyValue("rawText", rawText)
                    .addKeyValue("reason", "no_active_session")
                    .log("Track dialog message ignored");

                return false;
            });
    }
}
