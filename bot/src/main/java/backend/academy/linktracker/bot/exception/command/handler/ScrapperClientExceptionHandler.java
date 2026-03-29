package backend.academy.linktracker.bot.exception.command.handler;

import backend.academy.linktracker.bot.exception.client.ScrapperClientException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScrapperClientExceptionHandler implements CommandExceptionHandler{
    private final MessageService messageService;
    private final TelegramSender telegramSender;


    @Override
    public boolean supports(Exception ex) {
        return ex instanceof ScrapperClientException;
    }

    @Override
    public void handle(Exception ex, long chatId) {
        telegramSender.sendPlain(chatId, messageService.get("command.exception.scrapper-client-error"));
    }
}
