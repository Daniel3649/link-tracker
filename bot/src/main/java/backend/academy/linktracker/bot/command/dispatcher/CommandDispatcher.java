package backend.academy.linktracker.bot.command.dispatcher;

import backend.academy.linktracker.bot.command.Command;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import backend.academy.linktracker.bot.exception.command.UnknownCommandException;
import backend.academy.linktracker.bot.service.TrackConversationService;
import backend.academy.linktracker.bot.tracksession.DialogueState;
import com.pengrad.telegrambot.model.Update;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class CommandDispatcher {
    private final TrackConversationService trackConversationService;
    private final List<Command> commands;

    public void dispatch(String commandName, Update update) {
        long chatId = update.message().chat().id();

        Command matchedCommand = commands.stream()
            .filter(command -> command.name().equals(commandName))
            .findFirst()
            .orElseThrow(() -> new UnknownCommandException("Unknown command: " + commandName));

        if (trackConversationService.getDialogueState(chatId) != DialogueState.IDLE
            && !matchedCommand.name().equals("/cancel")) {
            trackConversationService.cancel(chatId);
        }

        matchedCommand.execute(update);
    }
}
