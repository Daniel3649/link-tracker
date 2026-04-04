package backend.academy.linktracker.scrapper.sender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.contract.grpc.BotUpdatesApiGrpc;
import backend.academy.linktracker.contract.grpc.LinkUpdateMessage;
import backend.academy.linktracker.scrapper.exception.client.BotClientException;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrpcLinkUpdateSenderTest {
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
    void shouldSendUpdateViaGrpc() {
        GrpcLinkUpdateSender sender = new GrpcLinkUpdateSender(BotUpdatesApiGrpc.newBlockingStub(channel));

        sender.send(new LinkUpdate(
                1L, URI.create("https://github.com/octocat/Hello-World"), "Repository changed", List.of(1001L, 1002L)));
    }

    @Test
    void shouldMapInvalidArgumentStatusToBotClientException() {
        GrpcLinkUpdateSender sender = new GrpcLinkUpdateSender(BotUpdatesApiGrpc.newBlockingStub(channel));

        assertThatThrownBy(() -> sender.send(new LinkUpdate(
                        99L, URI.create("https://github.com/octocat/Hello-World"), "bad update", List.of(1L))))
                .isInstanceOf(BotClientException.class)
                .hasMessageContaining("Bot rejected update");
    }

    private static final class TestBotUpdatesService extends BotUpdatesApiGrpc.BotUpdatesApiImplBase {
        @Override
        public void sendUpdate(LinkUpdateMessage request, StreamObserver<Empty> responseObserver) {
            if (request.getId() == 99L) {
                responseObserver.onError(Status.INVALID_ARGUMENT
                        .withDescription("Invalid update payload")
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }
}
