package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.command.meta.CommandName;
import backend.academy.linktracker.bot.command.support.CommandArgSupport;
import backend.academy.linktracker.bot.sender.TelegramSender;
import backend.academy.linktracker.bot.service.MessageService;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import com.pengrad.telegrambot.model.Update;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCommand implements Command {
    private final MessageService messageService;
    private final ScrapperClient scrapperClient;
    private final TelegramSender telegramSender;
    private final CommandArgSupport commandArgSupport;

    @Override
    public void execute(Update update) {
        if (update.message() == null || update.message().chat() == null) {
            return;
        }

        long chatId = update.message().chat().id();
        String rawText = update.message().text();
        String tagFilter = normalizeTagFilter(commandArgSupport.extractFirstArgument(rawText));

        ListLinksResponse response = scrapperClient.getLinks(chatId);
        List<LinkResponse> links = response.links() == null ? Collections.emptyList() : response.links();

        List<LinkResponse> filteredLinks = filterByTag(links, tagFilter);

        if (filteredLinks.isEmpty()) {
            telegramSender.sendPlain(
                    chatId,
                    tagFilter == null
                            ? messageService.get("command.list.empty")
                            : messageService.get("command.list.empty.by-tag", tagFilter));
            return;
        }

        telegramSender.sendPlain(chatId, buildListMessage(filteredLinks, tagFilter));
    }

    private String normalizeTagFilter(String tagFilter) {
        if (tagFilter == null) {
            return null;
        }

        String normalized = tagFilter.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public String name() {
        return CommandName.LIST.getText();
    }

    @Override
    public String description() {
        return messageService.get("command.list.description");
    }

    private List<LinkResponse> filterByTag(List<LinkResponse> links, String tagFilter) {
        if (tagFilter == null || tagFilter.isBlank()) {
            return links;
        }

        return links.stream()
                .filter(link -> link.tags() != null
                        && link.tags().stream()
                                .anyMatch(tag -> tag != null && tag.trim().equalsIgnoreCase(tagFilter.trim())))
                .toList();
    }

    private String buildListMessage(List<LinkResponse> links, String tagFilter) {
        String header = tagFilter == null
                ? messageService.get("command.list.header")
                : messageService.get("command.list.header.by-tag", tagFilter);

        String body = links.stream().map(this::formatLink).collect(Collectors.joining("\n\n"));

        return header + "\n\n" + body;
    }

    private String formatLink(LinkResponse link) {
        StringBuilder builder = new StringBuilder();

        builder.append("- ").append(link.url());

        if (link.tags() != null && !link.tags().isEmpty()) {
            builder.append("\n")
                    .append(messageService.get("command.list.tags-label"))
                    .append(": ")
                    .append(String.join(", ", link.tags()));
        }

        return builder.toString();
    }
}
