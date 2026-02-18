package backend.academy.linktracker.bot.command.dispatcher;

import backend.academy.linktracker.bot.command.Command;
import lombok.Getter;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Getter
public class CommandDispatcher {
    private final Map<String, Command> commandsByName;
    private final List<Command> commands;

    public CommandDispatcher(List<Command> commands) {
        commandsByName = commands.stream()
            .collect(Collectors.toUnmodifiableMap(
                Command::name,
                Function.identity()));
        this.commands = List.copyOf(commands);
    }

    public Optional<Command> getCommandByName(String name) {
        Objects.requireNonNull(name);
        var command = commandsByName.get(name);
        return Optional.ofNullable(command);
    }
}
