package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.conversation.TrackDialogState;
import java.util.Optional;

public interface TrackDialogStateRepository {
    Optional<TrackDialogState> findByChatId(long chatId);

    void save(long chatId, TrackDialogState state);

    boolean existsByChatId(long chatId);

    void deleteByChatId(long chatId);

    void clear();
}
