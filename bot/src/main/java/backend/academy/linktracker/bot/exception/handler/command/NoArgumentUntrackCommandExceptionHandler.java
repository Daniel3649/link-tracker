package backend.academy.linktracker.bot.exception.handler.command;

import backend.academy.linktracker.bot.exception.command.NoArgumentUntrackCommandException;
import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoArgumentUntrackCommandExceptionHandler implements ExceptionHandler {
    private final MessageService messageService;
    private final TelegramSender telegramSender;

    @Override
    public boolean supports(Exception ex) {
        return ex instanceof NoArgumentUntrackCommandException;
    }

    @Override
    public void handle(Exception ex, long chatId) {
        telegramSender.sendPlain(chatId, messageService.get("exception.no-argument-untrack-command"));
    }
}
