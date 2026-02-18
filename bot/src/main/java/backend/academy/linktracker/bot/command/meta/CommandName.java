package backend.academy.linktracker.bot.command.meta;

import lombok.Getter;

@Getter
public enum CommandName {
    START("/start"),
    HELP("/help");

    private String text;

    CommandName(String text) {
        this.text = text;
    }
}
