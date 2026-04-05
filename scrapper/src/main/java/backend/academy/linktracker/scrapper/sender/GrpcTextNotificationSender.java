package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.contract.dto.request.TextNotification;
import backend.academy.linktracker.contract.grpc.BotUpdatesApiGrpc;
import backend.academy.linktracker.contract.grpc.GrpcTransportMapper;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import backend.academy.linktracker.scrapper.logging.LogEvent;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bot", name = "transport", havingValue = "grpc")
@Slf4j
public class GrpcTextNotificationSender implements TextNotificationSender {
    private final BotUpdatesApiGrpc.BotUpdatesApiBlockingStub botUpdatesStub;

    @Override
    public void send(TextNotification notification) {
        try {
            botUpdatesStub.sendTextNotification(GrpcTransportMapper.toTextNotificationMessage(notification));
        } catch (StatusRuntimeException e) {
            log.atError()
                    .setCause(e)
                    .addKeyValue("event", LogEvent.BOT_RESPONSE_FAILED)
                    .addKeyValue("grpcStatus", e.getStatus().getCode())
                    .addKeyValue("grpcDescription", descriptionOrDefault(e))
                    .log("Bot returned gRPC error response");

            throw mapException(e);
        }
    }

    private RuntimeException mapException(StatusRuntimeException exception) {
        return switch (exception.getStatus().getCode()) {
            case INVALID_ARGUMENT ->
                new BotClientException("Bot rejected text notification: " + descriptionOrDefault(exception));
            case UNAVAILABLE, DEADLINE_EXCEEDED ->
                new BotClientException(
                        "Bot service error. gRPC status: " + exception.getStatus().getCode(),
                        exception);
            default ->
                new BotClientException(
                        "Unexpected bot gRPC response. Status: " + exception.getStatus().getCode(),
                        exception);
        };
    }

    private String descriptionOrDefault(StatusRuntimeException exception) {
        String description = exception.getStatus().getDescription();
        return description == null || description.isBlank()
                ? Status.fromThrowable(exception).getCode().name()
                : description;
    }
}
