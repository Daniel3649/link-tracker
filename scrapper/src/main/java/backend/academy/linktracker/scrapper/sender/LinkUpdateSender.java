package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.scrapper.dto.request.LinkUpdate;

public interface LinkUpdateSender {
    void send(LinkUpdate update);
}
