package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.bot", name = "transport", havingValue = "http", matchIfMissing = true)
public class HttpLinkUpdateSender extends AbstractHttpBotSender implements LinkUpdateSender {
    public HttpLinkUpdateSender(RestClient botRestClient, ObjectMapper objectMapper) {
        super(botRestClient, objectMapper, LoggerFactory.getLogger(HttpLinkUpdateSender.class));
    }

    @Override
    public void send(LinkUpdate update) {
        sendJson("/updates", update, "Bot rejected update: ");
    }
}
