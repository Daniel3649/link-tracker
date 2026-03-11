package backend.academy.linktracker.bot.command.meta;

import lombok.Getter;

@Getter
public enum CommandName {
    START("start"),
    HELP("help"),
    TRACK("track"),
    CANCEL("cancel");

    private final String text;

    CommandName(String text) {
        this.text = text;
    }
}
