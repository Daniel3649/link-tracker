package backend.academy.linktracker.contract.grpc;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.request.TextNotification;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class GrpcTransportMapper {
    private GrpcTransportMapper() {}

    public static ChatCommand toChatCommand(long chatId) {
        return ChatCommand.newBuilder()
                .setChatId(requirePositive("chatId", chatId))
                .build();
    }

    public static long toChatId(ChatCommand command) {
        return requirePositive("chatId", command.getChatId());
    }

    public static long toChatId(ListLinksCommand command) {
        return requirePositive("chatId", command.getChatId());
    }

    public static AddLinkCommand toAddLinkCommand(long chatId, AddLinkRequest request) {
        requirePositive("chatId", chatId);
        requireNonNull("request", request);

        return AddLinkCommand.newBuilder()
                .setChatId(chatId)
                .setLink(requireUriText(request.link()))
                .addAllTags(validateTextSet("tags", request.tags()))
                .addAllFilters(validateTextList("filters", request.filters()))
                .build();
    }

    public static AddLinkRequest toAddLinkRequest(AddLinkCommand command) {
        requirePositive("chatId", command.getChatId());

        return new AddLinkRequest(
                parseUri(command.getLink()),
                validateTextSet("tags", Set.copyOf(command.getTagsList())),
                validateTextList("filters", command.getFiltersList()));
    }

    public static long toChatId(AddLinkCommand command) {
        return requirePositive("chatId", command.getChatId());
    }

    public static RemoveLinkCommand toRemoveLinkCommand(long chatId, RemoveLinkRequest request) {
        requirePositive("chatId", chatId);
        requireNonNull("request", request);

        return RemoveLinkCommand.newBuilder()
                .setChatId(chatId)
                .setLink(requireUriText(request.link()))
                .build();
    }

    public static RemoveLinkRequest toRemoveLinkRequest(RemoveLinkCommand command) {
        requirePositive("chatId", command.getChatId());
        return new RemoveLinkRequest(parseUri(command.getLink()));
    }

    public static long toChatId(RemoveLinkCommand command) {
        return requirePositive("chatId", command.getChatId());
    }

    public static ListLinksCommand toListLinksCommand(long chatId) {
        return ListLinksCommand.newBuilder()
                .setChatId(requirePositive("chatId", chatId))
                .build();
    }

    public static LinkResponseMessage toLinkResponseMessage(LinkResponse response) {
        requireNonNull("response", response);

        return LinkResponseMessage.newBuilder()
                .setId(requirePositive("id", response.id()))
                .setUrl(requireUriText(response.url()))
                .addAllTags(validateTextList("tags", response.tags()))
                .addAllFilters(validateTextList("filters", response.filters()))
                .build();
    }

    public static LinkResponse toLinkResponse(LinkResponseMessage message) {
        return new LinkResponse(
                requirePositive("id", message.getId()),
                parseUri(message.getUrl()),
                validateTextList("tags", message.getTagsList()),
                validateTextList("filters", message.getFiltersList()));
    }

    public static ListLinksResponseMessage toListLinksResponseMessage(ListLinksResponse response) {
        requireNonNull("response", response);

        List<LinkResponseMessage> links = response.links() == null
                ? List.of()
                : response.links().stream()
                        .map(GrpcTransportMapper::toLinkResponseMessage)
                        .toList();

        return ListLinksResponseMessage.newBuilder()
                .addAllLinks(links)
                .setSize(response.size() == null ? links.size() : response.size())
                .build();
    }

    public static ListLinksResponse toListLinksResponse(ListLinksResponseMessage message) {
        List<LinkResponse> links = message.getLinksList().stream()
                .map(GrpcTransportMapper::toLinkResponse)
                .toList();
        return new ListLinksResponse(links, message.getSize());
    }

    public static LinkUpdateMessage toLinkUpdateMessage(LinkUpdate update) {
        requireNonNull("update", update);

        return LinkUpdateMessage.newBuilder()
                .setId(requirePositive("id", update.id()))
                .setUrl(requireUriText(update.url()))
                .setDescription(requireNonBlank("description", update.description()))
                .addAllTgChatIds(validatePositiveList("tgChatIds", update.tgChatIds()))
                .build();
    }

    public static LinkUpdate toLinkUpdate(LinkUpdateMessage message) {
        List<Long> tgChatIds = validatePositiveList("tgChatIds", message.getTgChatIdsList());
        if (tgChatIds.isEmpty()) {
            throw new IllegalArgumentException("tgChatIds must not be empty");
        }

        return new LinkUpdate(
                requirePositive("id", message.getId()),
                parseUri(message.getUrl()),
                requireNonBlank("description", message.getDescription()),
                tgChatIds);
    }

    public static TextNotificationMessage toTextNotificationMessage(TextNotification notification) {
        requireNonNull("notification", notification);

        return TextNotificationMessage.newBuilder()
                .setMessage(requireNonBlank("message", notification.message()))
                .addAllTgChatIds(validatePositiveList("tgChatIds", notification.tgChatIds()))
                .build();
    }

    public static TextNotification toTextNotification(TextNotificationMessage message) {
        List<Long> tgChatIds = validatePositiveList("tgChatIds", message.getTgChatIdsList());
        if (tgChatIds.isEmpty()) {
            throw new IllegalArgumentException("tgChatIds must not be empty");
        }

        return new TextNotification(requireNonBlank("message", message.getMessage()), tgChatIds);
    }

    private static String requireUriText(URI uri) {
        requireNonNull("uri", uri);
        return requireNonBlank("uri", uri.toString());
    }

    private static URI parseUri(String value) {
        return URI.create(requireNonBlank("uri", value));
    }

    private static long requirePositive(String fieldName, Long value) {
        requireNonNull(fieldName, value);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireNonBlank(String fieldName, String value) {
        requireNonNull(fieldName, value);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static <T> T requireNonNull(String fieldName, T value) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static List<String> validateTextList(String fieldName, List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream().map(value -> requireNonBlank(fieldName, value)).toList();
    }

    private static Set<String> validateTextSet(String fieldName, Set<String> values) {
        if (values == null) {
            return Set.of();
        }

        return values.stream().map(value -> requireNonBlank(fieldName, value)).collect(Collectors.toUnmodifiableSet());
    }

    private static List<Long> validatePositiveList(String fieldName, List<Long> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream().map(value -> requirePositive(fieldName, value)).toList();
    }
}
