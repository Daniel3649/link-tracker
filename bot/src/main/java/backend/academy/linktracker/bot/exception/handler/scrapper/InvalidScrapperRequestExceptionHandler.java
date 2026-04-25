package backend.academy.linktracker.bot.exception.handler.scrapper;

import backend.academy.linktracker.bot.exception.client.InvalidScrapperRequestException;
import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvalidScrapperRequestExceptionHandler implements ExceptionHandler {
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public boolean supports(Exception ex) {
        return ex instanceof InvalidScrapperRequestException;
    }

    @Override
    public void handle(Exception ex, long chatId) {
        telegramSender.sendPlain(chatId, messageService.get("exception.invalid-request"));
    }
}
