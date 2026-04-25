package backend.academy.linktracker.bot.exception.handler.link;

import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.exception.link.LinkNotTrackedException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkNotTrackedExceptionHandler implements ExceptionHandler {
    private final MessageService messageService;
    private final TelegramSender telegramSender;

    @Override
    public boolean supports(Exception ex) {
        return ex instanceof LinkNotTrackedException;
    }

    @Override
    public void handle(Exception ex, long chatId) {
        telegramSender.sendPlain(chatId, messageService.get("exception.link-not-found"));
    }
}
