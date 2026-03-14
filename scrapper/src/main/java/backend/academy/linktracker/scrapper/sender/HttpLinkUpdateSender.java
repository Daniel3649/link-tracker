package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import backend.academy.linktracker.scrapper.clients.bot.BotUpdatesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HttpLinkUpdateSender implements LinkUpdateSender {
    private final BotUpdatesClient botUpdatesClient;

    @Override
    public void send(LinkUpdate update) {
        botUpdatesClient.sendUpdate(update);
    }
}
