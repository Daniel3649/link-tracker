package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.contract.grpc.ScrapperApiGrpc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
@ConditionalOnProperty(prefix = "app.scrapper", name = "transport", havingValue = "grpc")
public class GrpcScrapperClientConfig {
    @Bean
    public ScrapperApiGrpc.ScrapperApiBlockingStub scrapperApiBlockingStub(GrpcChannelFactory channels) {
        return ScrapperApiGrpc.newBlockingStub(channels.createChannel("scrapper"));
    }
}
