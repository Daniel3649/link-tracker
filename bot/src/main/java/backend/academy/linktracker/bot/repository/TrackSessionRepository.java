package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.tracksession.TrackSession;
import java.util.Optional;

public interface TrackSessionRepository {
    Optional<TrackSession> findByChatId(long chatId);

    boolean existsByChatId(long chatId);

    void save(long chatId, TrackSession trackSession);

    void deleteByChatId(long chatId);
}
