package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.exception.tracksession.IllegalTrackStateException;
import backend.academy.linktracker.bot.exception.tracksession.TrackSessionNotFoundException;
import backend.academy.linktracker.bot.tracksession.CancelTrackResult;
import backend.academy.linktracker.bot.tracksession.DialogueState;
import backend.academy.linktracker.bot.tracksession.TrackSession;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import backend.academy.linktracker.bot.repository.TrackSessionRepository;
import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class TrackConversationService {
    private final TrackSessionRepository trackSessionRepository;
    private final ScrapperClient  scrapperClient;

    public void acceptLink(long chatId, @NotNull URI link) {
        TrackSession session = trackSessionRepository.findByChatId(chatId)
            .orElseThrow(() -> new TrackSessionNotFoundException("No track session found for chat id " + chatId));

        if (session.state() != DialogueState.WAITING_LINK) {
            throw new IllegalTrackStateException("Chat " + chatId + " is not waiting for link");
        }

        trackSessionRepository.save(chatId, TrackSession.waitingTags(link));
    }

    public void acceptTags(long chatId, Set<String> tags) {
        TrackSession session = trackSessionRepository.findByChatId(chatId)
            .orElseThrow(() -> new TrackSessionNotFoundException("No track session found for chat id " + chatId));

        if (session.state() != DialogueState.WAITING_TAGS) {
            throw new IllegalTrackStateException("Chat " + chatId + " is not waiting for tags");
        }

        URI link = session.link();
        if (link == null) {
            trackSessionRepository.deleteByChatId(chatId);
            throw new IllegalTrackStateException("Pending link is missing for chat id " + chatId);
        }

        AddLinkRequest request = new AddLinkRequest(link, tags, List.of());
        scrapperClient.addLink(chatId, request);
        trackSessionRepository.deleteByChatId(chatId);
    }

    public DialogueState getDialogueState(long chatId) {
        return trackSessionRepository.findByChatId(chatId)
            .map(TrackSession::state)
            .orElse(DialogueState.IDLE);
    }

    public void start(long chatId) {
        trackSessionRepository.save(chatId, TrackSession.waitingLink());
    }

    public CancelTrackResult cancel(long chatId) {
        Optional<TrackSession> session = trackSessionRepository.findByChatId(chatId);

        if (session.isEmpty()) {
            return CancelTrackResult.NO_ACTIVE_SESSION;
        }

        trackSessionRepository.deleteByChatId(chatId);
        return CancelTrackResult.CANCELLED;
    }
}
