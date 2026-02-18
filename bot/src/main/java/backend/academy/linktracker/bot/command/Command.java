package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;

public interface Command {
    void execute(Update update);
    String name();
    String description();
}
