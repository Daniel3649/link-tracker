package backend.academy.linktracker.bot.exception.command.handler;

public interface CommandExceptionHandler {
    boolean supports(Exception ex);
    void handle(Exception ex, long chatId);
}
