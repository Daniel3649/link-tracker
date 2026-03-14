package backend.academy.linktracker.bot.conversation.handler;

import backend.academy.linktracker.bot.conversation.TrackDialogState;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackStepHandlerRegistry {
    private final List<TrackStepHandler> handlers;

    public TrackStepHandler getHandler(TrackDialogState state) {
        return handlers.stream()
                .filter(handler -> handler.supports(state))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No handler found for state: " + state.step()));
    }
}
