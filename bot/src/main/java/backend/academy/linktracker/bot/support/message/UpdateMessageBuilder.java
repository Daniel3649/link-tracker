package backend.academy.linktracker.bot.support.message;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import org.springframework.stereotype.Component;

@Component
public class UpdateMessageBuilder {
    public String buildMessage(LinkUpdate update) {
        StringBuilder builder = new StringBuilder();

        builder.append("Link update").append('\n')
            .append(update.url());

        if (update.description() != null && !update.description().isBlank()) {
            builder.append('\n').append('\n')
                .append(update.description());
        }

        return builder.toString();
    }
}
