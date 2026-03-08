package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.scrapper.dto.request.LinkUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LinkUpdateSender {
    public void send(LinkUpdate update) {
        log.info("Stub send LinkUpdate: {}", update);
    }
}
