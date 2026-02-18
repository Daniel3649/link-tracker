package backend.academy.linktracker.bot.command;

import lombok.Getter;

@Getter
public enum CommandName {
    START("/start");

    private String text;

    CommandName(String text) {
        this.text = text;
    }
}
