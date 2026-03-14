package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.repository.TrackDialogStateRepository;
import backend.academy.linktracker.bot.conversation.TrackDialogState;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryTrackDialogStateRepository implements TrackDialogStateRepository {
    private final ConcurrentMap<Long, TrackDialogState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<TrackDialogState> findByChatId(long chatId) {
        return Optional.ofNullable(states.get(chatId));
    }

    @Override
    public void save(long chatId, TrackDialogState state) {
        states.put(chatId, state);
    }

    @Override
    public boolean existsByChatId(long chatId) {
        return states.containsKey(chatId);
    }

    @Override
    public void deleteByChatId(long chatId) {
        states.remove(chatId);
    }

    @Override
    public void clear() {
        states.clear();
    }


}
