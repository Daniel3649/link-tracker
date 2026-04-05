package backend.academy.linktracker.bot.grpc;

import backend.academy.linktracker.bot.service.LinkUpdateNotificationService;
import backend.academy.linktracker.bot.service.TextNotificationService;
import backend.academy.linktracker.contract.grpc.BotUpdatesApiGrpc;
import backend.academy.linktracker.contract.grpc.GrpcTransportMapper;
import backend.academy.linktracker.contract.grpc.LinkUpdateMessage;
import backend.academy.linktracker.contract.grpc.TextNotificationMessage;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class BotUpdatesGrpcService extends BotUpdatesApiGrpc.BotUpdatesApiImplBase {
    private final LinkUpdateNotificationService notificationService;
    private final TextNotificationService textNotificationService;

    @Override
    public void sendUpdate(LinkUpdateMessage request, StreamObserver<Empty> responseObserver) {
        try {
            notificationService.sendNotification(GrpcTransportMapper.toLinkUpdate(request));
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to process link update")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void sendTextNotification(TextNotificationMessage request, StreamObserver<Empty> responseObserver) {
        try {
            textNotificationService.sendNotification(GrpcTransportMapper.toTextNotification(request));
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to process text notification")
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
