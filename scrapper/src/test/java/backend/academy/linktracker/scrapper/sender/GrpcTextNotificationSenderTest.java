package backend.academy.linktracker.scrapper.sender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.contract.dto.request.TextNotification;
import backend.academy.linktracker.contract.grpc.BotUpdatesApiGrpc;
import backend.academy.linktracker.contract.grpc.TextNotificationMessage;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrpcTextNotificationSenderTest {
    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new TestBotUpdatesService())
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void shouldSendTextNotificationViaGrpc() {
        GrpcTextNotificationSender sender = new GrpcTextNotificationSender(BotUpdatesApiGrpc.newBlockingStub(channel));

        sender.send(new TextNotification("Link check report", List.of(1001L, 1002L)));
    }

    @Test
    void shouldMapInvalidArgumentStatusToBotClientException() {
        GrpcTextNotificationSender sender = new GrpcTextNotificationSender(BotUpdatesApiGrpc.newBlockingStub(channel));

        assertThatThrownBy(() -> sender.send(new TextNotification("bad notification", List.of(1L))))
                .isInstanceOf(BotClientException.class)
                .hasMessageContaining("Bot rejected text notification");
    }

    private static final class TestBotUpdatesService extends BotUpdatesApiGrpc.BotUpdatesApiImplBase {
        @Override
        public void sendTextNotification(TextNotificationMessage request, StreamObserver<Empty> responseObserver) {
            if ("bad notification".equals(request.getMessage())) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid text notification payload")
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }
}
