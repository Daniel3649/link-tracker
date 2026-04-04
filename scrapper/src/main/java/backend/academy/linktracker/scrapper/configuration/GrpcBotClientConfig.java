package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.contract.grpc.BotUpdatesApiGrpc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
@ConditionalOnProperty(prefix = "app.bot", name = "transport", havingValue = "grpc")
public class GrpcBotClientConfig {
    @Bean
    public BotUpdatesApiGrpc.BotUpdatesApiBlockingStub botUpdatesApiBlockingStub(GrpcChannelFactory channels) {
        return BotUpdatesApiGrpc.newBlockingStub(channels.createChannel("bot"));
    }
}
