package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.repository.TrackSessionRepository;
import backend.academy.linktracker.bot.tracksession.TrackSession;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTrackSessionRepository implements TrackSessionRepository {
    private final Map<Long, TrackSession> trackSessions = new ConcurrentHashMap<>();

    @Override
    public Optional<TrackSession> findByChatId(long chatId) {
        return Optional.ofNullable(trackSessions.get(chatId));
    }

    @Override
    public boolean existsByChatId(long chatId) {
        return trackSessions.containsKey(chatId);
    }

    @Override
    public void save(long chatId, TrackSession trackSession) {
        trackSessions.put(chatId, trackSession);
    }

    @Override
    public void deleteByChatId(long chatId) {
        trackSessions.remove(chatId);
    }
}
