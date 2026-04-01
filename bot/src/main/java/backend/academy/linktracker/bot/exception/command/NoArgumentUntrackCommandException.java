package backend.academy.linktracker.bot.exception.command;

public class NoArgumentUntrackCommandException extends RuntimeException {
    public NoArgumentUntrackCommandException(String message) {
        super(message);
    }
}
