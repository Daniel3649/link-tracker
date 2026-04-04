package backend.academy.linktracker.scrapper.grpc;

import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.contract.grpc.AddLinkCommand;
import backend.academy.linktracker.contract.grpc.ChatCommand;
import backend.academy.linktracker.contract.grpc.GrpcTransportMapper;
import backend.academy.linktracker.contract.grpc.LinkResponseMessage;
import backend.academy.linktracker.contract.grpc.ListLinksCommand;
import backend.academy.linktracker.contract.grpc.ListLinksResponseMessage;
import backend.academy.linktracker.contract.grpc.RemoveLinkCommand;
import backend.academy.linktracker.contract.grpc.ScrapperApiGrpc;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.chat.TelegramChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.exception.link.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.service.SubscriptionService;
import backend.academy.linktracker.scrapper.service.TelegramChatService;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class ScrapperGrpcService extends ScrapperApiGrpc.ScrapperApiImplBase {
    private final TelegramChatService telegramChatService;
    private final SubscriptionService subscriptionService;

    @Override
    public void registerChat(ChatCommand request, StreamObserver<Empty> responseObserver) {
        handleEmpty(responseObserver, () -> telegramChatService.registerChat(GrpcTransportMapper.toChatId(request)));
    }

    @Override
    public void unregisterChat(ChatCommand request, StreamObserver<Empty> responseObserver) {
        handleEmpty(responseObserver, () -> telegramChatService.unregisterChat(GrpcTransportMapper.toChatId(request)));
    }

    @Override
    public void addLink(AddLinkCommand request, StreamObserver<LinkResponseMessage> responseObserver) {
        handle(responseObserver, () -> {
            LinkResponse response = subscriptionService.addSubscription(
                    GrpcTransportMapper.toChatId(request), GrpcTransportMapper.toAddLinkRequest(request));
            return GrpcTransportMapper.toLinkResponseMessage(response);
        });
    }

    @Override
    public void removeLink(RemoveLinkCommand request, StreamObserver<LinkResponseMessage> responseObserver) {
        handle(responseObserver, () -> {
            LinkResponse response = subscriptionService.removeSubscription(
                    GrpcTransportMapper.toChatId(request), GrpcTransportMapper.toRemoveLinkRequest(request));
            return GrpcTransportMapper.toLinkResponseMessage(response);
        });
    }

    @Override
    public void listLinks(ListLinksCommand request, StreamObserver<ListLinksResponseMessage> responseObserver) {
        handle(responseObserver, () -> {
            ListLinksResponse response = subscriptionService.getAllSubscriptions(GrpcTransportMapper.toChatId(request));
            return GrpcTransportMapper.toListLinksResponseMessage(response);
        });
    }

    private void handleEmpty(StreamObserver<Empty> responseObserver, Runnable action) {
        handle(responseObserver, () -> {
            action.run();
            return Empty.getDefaultInstance();
        });
    }

    private <T> void handle(StreamObserver<T> responseObserver, Supplier<T> supplier) {
        try {
            responseObserver.onNext(supplier.get());
            responseObserver.onCompleted();
        } catch (RuntimeException e) {
            responseObserver.onError(toStatusException(e));
        }
    }

    private StatusRuntimeException toStatusException(RuntimeException exception) {
        Status status = switch (exception) {
            case IllegalArgumentException _, UnsupportedLinkException _ -> Status.INVALID_ARGUMENT;
            case TelegramChatAlreadyExistsException _, SubscriptionAlreadyExistsException _ -> Status.ALREADY_EXISTS;
            case TelegramChatNotFoundException _, SubscriptionNotFoundException _ -> Status.NOT_FOUND;
            case RepositoryPollingException _ -> Status.UNAVAILABLE;
            default -> Status.INTERNAL;
        };

        String description = status == Status.INTERNAL ? "Internal scrapper error" : exception.getMessage();

        return status.withDescription(description).withCause(exception).asRuntimeException();
    }
}
