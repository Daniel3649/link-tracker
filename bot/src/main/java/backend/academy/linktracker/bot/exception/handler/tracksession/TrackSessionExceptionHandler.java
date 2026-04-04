package backend.academy.linktracker.bot.exception.handler.tracksession;

import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.exception.tracksession.IllegalTrackStateException;
import backend.academy.linktracker.bot.exception.tracksession.TrackSessionNotFoundException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackSessionExceptionHandler implements ExceptionHandler {
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public boolean supports(Exception ex) {
        return ex instanceof TrackSessionNotFoundException || ex instanceof IllegalTrackStateException;
    }

    @Override
    public void handle(Exception ex, long chatId) {
        telegramSender.sendPlain(chatId, messageService.get("exception.track-session"));
    }
}
