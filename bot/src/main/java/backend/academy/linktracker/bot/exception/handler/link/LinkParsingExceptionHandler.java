package backend.academy.linktracker.bot.exception.handler.link;

import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.exception.link.LinkParsingException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LinkParsingExceptionHandler implements ExceptionHandler {
    private final MessageService messageService;
    private final TelegramSender telegramSender;


    @Override
    public boolean supports(Exception ex) {
        return ex instanceof LinkParsingException;
    }

    @Override
    public void handle(Exception ex, long chatId) {
        telegramSender.sendPlain(chatId, messageService.get("exception.link-parsing"));
    }
}
