package backend.academy.linktracker.scrapper.sender;

import backend.academy.linktracker.contract.dto.request.TextNotification;

public interface TextNotificationSender {
    void send(TextNotification notification);
}
