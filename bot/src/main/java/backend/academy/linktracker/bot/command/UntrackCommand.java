package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.command.support.CommandArgSupport;
import backend.academy.linktracker.bot.exception.client.InvalidScrapperRequestException;
import backend.academy.linktracker.bot.exception.client.ScrapperClientException;
import backend.academy.linktracker.bot.exception.client.ScrapperUnavailableException;
import backend.academy.linktracker.bot.exception.command.NoArgumentUntrackCommandException;
import backend.academy.linktracker.bot.exception.link.LinkNotTrackedException;
import backend.academy.linktracker.bot.exception.link.LinkParsingException;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.link.common.ParsedSupportedLink;
import backend.academy.linktracker.contract.link.exception.UnsupportedLinkFormatException;
import backend.academy.linktracker.contract.link.parser.SupportedLinkParser;
import com.pengrad.telegrambot.model.Update;
import java.net.URI;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@RequiredArgsConstructor
@Validated
public class UntrackCommand implements Command {
    private final MessageService messageService;
    private final SupportedLinkParser supportedLinkParser;
    private final ScrapperClient scrapperClient;
    private final TelegramSender telegramSender;
    private final CommandArgSupport commandArgSupport;

    @Override
    public void execute(@NotNull Update update) {
        long chatId = update.message().chat().id();
        String rawText = update.message().text();

        String linkArgument = commandArgSupport.extractFirstArgument(rawText);
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
