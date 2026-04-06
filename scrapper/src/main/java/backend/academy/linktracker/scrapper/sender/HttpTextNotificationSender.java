package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.contract.dto.request.TextNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.bot", name = "transport", havingValue = "http", matchIfMissing = true)
public class HttpTextNotificationSender extends AbstractHttpBotSender implements TextNotificationSender {
    public HttpTextNotificationSender(RestClient botRestClient, ObjectMapper objectMapper) {
        super(botRestClient, objectMapper, LoggerFactory.getLogger(HttpTextNotificationSender.class));
    }

    @Override
    public void send(TextNotification notification) {
        sendJson("/notifications/text", notification, "Bot rejected text notification: ");
    }
}
