package backend.academy.linktracker.bot.command.dispatcher;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.exception.command.UnknownCommandException;
import backend.academy.linktracker.bot.logging.LogEvent;
import backend.academy.linktracker.bot.service.TrackConversationService;
import backend.academy.linktracker.bot.tracksession.DialogueState;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
@Slf4j
public class CommandDispatcher {
    private final TrackConversationService trackConversationService;
    private final List<Command> commands;

    public void dispatch(String commandName, Update update) {
        long chatId = update.message().chat().id();
        DialogueState dialogueState = trackConversationService.getDialogueState(chatId);

        Command matchedCommand = commands.stream()
                .filter(command -> command.name().equals(commandName))
                .findFirst()
                .orElseThrow(() -> new UnknownCommandException("Unknown command: " + commandName));

        boolean interruptedDialogue = dialogueState != DialogueState.IDLE && !matchedCommand.name().equals("cancel");
        if (interruptedDialogue) {
            trackConversationService.cancel(chatId);
        }

        log.atInfo()
                .addKeyValue("event", LogEvent.COMMAND_DISPATCH)
                .addKeyValue("command", matchedCommand.name())
                .addKeyValue("interruptedDialogue", interruptedDialogue)
                .log("Command dispatched");

        matchedCommand.execute(update);
    }
}
