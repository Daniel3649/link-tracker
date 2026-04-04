package backend.academy.linktracker.bot.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.bot.exception.chat.ChatAlreadyRegisteredException;
import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.grpc.AddLinkCommand;
import backend.academy.linktracker.contract.grpc.ChatCommand;
import backend.academy.linktracker.contract.grpc.LinkResponseMessage;
import backend.academy.linktracker.contract.grpc.ScrapperApiGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrpcScrapperClientTest {
    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new TestScrapperService())
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
    void shouldReturnLinkResponseFromGrpcService() {
        GrpcScrapperClient client =
                new GrpcScrapperClient(ScrapperApiGrpc.newBlockingStub(channel));

        LinkResponse response = client.addLink(
                101L,
                new AddLinkRequest(URI.create("https://github.com/octocat/Hello-World"), Set.of("work"), List.of("new")));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.url()).isEqualTo(URI.create("https://github.com/octocat/Hello-World"));
        assertThat(response.tags()).containsExactly("work");
        assertThat(response.filters()).containsExactly("new");
    }

    @Test
    void shouldMapAlreadyExistsStatusForRegisterChat() {
        GrpcScrapperClient client =
                new GrpcScrapperClient(ScrapperApiGrpc.newBlockingStub(channel));

        assertThatThrownBy(() -> client.registerChat(999L))
                .isInstanceOf(ChatAlreadyRegisteredException.class)
                .hasMessageContaining("Chat already exists");
    }

    private static final class TestScrapperService extends ScrapperApiGrpc.ScrapperApiImplBase {
        @Override
        public void registerChat(ChatCommand request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
            if (request.getChatId() == 999L) {
                responseObserver.onError(Status.ALREADY_EXISTS
                        .withDescription("Chat already exists")
                        .asRuntimeException());
                return;
            }

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void addLink(AddLinkCommand request, StreamObserver<LinkResponseMessage> responseObserver) {
            responseObserver.onNext(LinkResponseMessage.newBuilder()
                    .setId(42L)
                    .setUrl(request.getLink())
                    .addAllTags(request.getTagsList())
                    .addAllFilters(request.getFiltersList())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
