package backend.academy.linktracker.bot.command.dispatcher;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandName;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CommandDispatcher {
    private final Map<String, Command> commandsByName;

    public CommandDispatcher(List<Command> commands) {
        commandsByName = commands.stream()
            .collect(Collectors.toMap(
                Command::name,
                Function.identity()));
    }

    public Optional<Command> getCommandByName(String name) {
        Objects.requireNonNull(name, "Command name is null");
        var command = commandsByName.get(name);
        return Optional.ofNullable(command);
    }
}
