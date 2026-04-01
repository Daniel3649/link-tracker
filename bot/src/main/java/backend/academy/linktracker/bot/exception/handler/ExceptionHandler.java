package backend.academy.linktracker.bot.exception.handler;

public interface ExceptionHandler {
    boolean supports(Exception ex);
    void handle(Exception ex, long chatId);
}
