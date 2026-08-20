package backend.academy.linktracker.bot.support.message;

import backend.academy.linktracker.contract.dto.request.LinkUpdate;

public final class UpdateMessageBuilder {
    private UpdateMessageBuilder() {}

    public static String buildMessage(LinkUpdate update) {
        StringBuilder builder = new StringBuilder(update.url().toString());

        if (update.description() != null && !update.description().isBlank()) {
            builder.append('\n').append('\n').append(update.description());
        }

        return builder.toString();
    }
}
