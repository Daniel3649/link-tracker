package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.command.support.CommandArgSupport;
import backend.academy.linktracker.bot.exception.command.NoArgumentUntrackCommandException;
import backend.academy.linktracker.bot.exception.link.LinkParsingException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import com.pengrad.telegrambot.model.Update;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UntrackCommand implements Command {
    private final MessageService messageService;
    private final ScrapperClient scrapperClient;
    private final TelegramSender telegramSender;

    @Override
    public void execute(Update update) {
        long chatId = update.message().chat().id();
        String rawText = update.message().text();

        String linkArgument = CommandArgSupport.extractFirstArgument(rawText);
        if (linkArgument == null) {
            throw new NoArgumentUntrackCommandException("No link argument was provided");
        }

        URI link;
        try {
            link = URI.create(linkArgument);
        } catch (IllegalArgumentException e) {
            throw new LinkParsingException("Invalid link argument: " + linkArgument);
        }

        RemoveLinkRequest request = new RemoveLinkRequest(link);
        scrapperClient.removeLink(chatId, request);
        telegramSender.sendPlain(chatId, messageService.get("command.untrack.success"));
    }

    @Override
    public String name() {
        return CommandName.UNTRACK.getText();
    }

    @Override
    public String description() {
        return messageService.get("command.untrack.description");
    }
}
