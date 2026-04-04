package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.exception.chat.ChatAlreadyRegisteredException;
import backend.academy.linktracker.bot.exception.chat.ChatNotRegisteredException;
import backend.academy.linktracker.bot.exception.client.InvalidScrapperRequestException;
import backend.academy.linktracker.bot.exception.client.ScrapperUnavailableException;
import backend.academy.linktracker.bot.exception.link.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exception.link.LinkNotTrackedException;
import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.contract.grpc.GrpcTransportMapper;
import backend.academy.linktracker.contract.grpc.ScrapperApiGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.scrapper", name = "transport", havingValue = "grpc")
public class GrpcScrapperClient implements ScrapperClient {
    private final ScrapperApiGrpc.ScrapperApiBlockingStub scrapperStub;

    @Override
    public void registerChat(long chatId) {
        try {
            scrapperStub.registerChat(GrpcTransportMapper.toChatCommand(chatId));
        } catch (StatusRuntimeException e) {
            throw mapRegisterChatException(e);
        }
    }

    @Override
    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        try {
            return GrpcTransportMapper.toLinkResponse(
                    scrapperStub.addLink(GrpcTransportMapper.toAddLinkCommand(chatId, request)));
        } catch (StatusRuntimeException e) {
            throw mapAddLinkException(e);
        }
    }

    @Override
    public LinkResponse removeLink(long chatId, RemoveLinkRequest request) {
        try {
            return GrpcTransportMapper.toLinkResponse(
                    scrapperStub.removeLink(GrpcTransportMapper.toRemoveLinkCommand(chatId, request)));
        } catch (StatusRuntimeException e) {
            throw mapRemoveLinkException(e);
        }
    }

    @Override
    public ListLinksResponse getLinks(long chatId) {
        try {
            return GrpcTransportMapper.toListLinksResponse(
                    scrapperStub.listLinks(GrpcTransportMapper.toListLinksCommand(chatId)));
        } catch (StatusRuntimeException e) {
            throw mapGetLinksException(e);
        }
    }

    private RuntimeException mapRegisterChatException(StatusRuntimeException exception) {
        return switch (exception.getStatus().getCode()) {
            case INVALID_ARGUMENT ->
                new InvalidScrapperRequestException(descriptionOrDefault(exception, "Invalid request"));
            case ALREADY_EXISTS ->
                new ChatAlreadyRegisteredException(descriptionOrDefault(exception, "Chat already exists"));
            default -> unavailable(exception);
        };
    }

    private RuntimeException mapAddLinkException(StatusRuntimeException exception) {
        return switch (exception.getStatus().getCode()) {
            case INVALID_ARGUMENT ->
                new InvalidScrapperRequestException(descriptionOrDefault(exception, "Invalid request"));
            case NOT_FOUND -> new ChatNotRegisteredException(descriptionOrDefault(exception, "Chat not found"));
            case ALREADY_EXISTS ->
                new LinkAlreadyTrackedException(descriptionOrDefault(exception, "Link already tracked"));
            default -> unavailable(exception);
        };
    }

    private RuntimeException mapRemoveLinkException(StatusRuntimeException exception) {
        return switch (exception.getStatus().getCode()) {
            case INVALID_ARGUMENT ->
                new InvalidScrapperRequestException(descriptionOrDefault(exception, "Invalid request"));
            case NOT_FOUND -> new LinkNotTrackedException(descriptionOrDefault(exception, "Subscription not found"));
            default -> unavailable(exception);
        };
    }

    private RuntimeException mapGetLinksException(StatusRuntimeException exception) {
        return switch (exception.getStatus().getCode()) {
            case INVALID_ARGUMENT ->
                new InvalidScrapperRequestException(descriptionOrDefault(exception, "Invalid request"));
            case NOT_FOUND -> new ChatNotRegisteredException(descriptionOrDefault(exception, "Chat not found"));
            default -> unavailable(exception);
        };
    }

    private ScrapperUnavailableException unavailable(StatusRuntimeException exception) {
        Status.Code code = exception.getStatus().getCode();
        return new ScrapperUnavailableException("Unexpected scrapper gRPC response. Status: " + code, exception);
    }

    private String descriptionOrDefault(StatusRuntimeException exception, String fallback) {
        String description = exception.getStatus().getDescription();
        return description == null || description.isBlank() ? fallback : description;
    }
}
