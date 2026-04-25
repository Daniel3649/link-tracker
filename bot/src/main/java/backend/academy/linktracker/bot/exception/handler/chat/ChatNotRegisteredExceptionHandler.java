package backend.academy.linktracker.bot.exception.handler.chat;

import backend.academy.linktracker.bot.exception.chat.ChatNotRegisteredException;
import backend.academy.linktracker.bot.exception.handler.ExceptionHandler;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatNotRegisteredExceptionHandler implements ExceptionHandler {
    private final TelegramSender telegramSender;
    private final MessageService messageService;

    @Override
    public boolean supports(Exception ex) {
        return ex instanceof ChatNotRegisteredException;
    }

    @Override
    public void handle(Exception ex, long chatId) {
        telegramSender.sendPlain(chatId, messageService.get("exception.chat-not-registered"));
    }
}
