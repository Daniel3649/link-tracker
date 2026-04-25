package backend.academy.linktracker.bot.command.support;

import org.springframework.stereotype.Component;

@Component
public class CommandArgSupport {
    public String extractFirstArgument(String rawText) {
        if (rawText == null) {
            return null;
        }

        String trimmed = rawText.trim();
        String[] parts = trimmed.split("\\s+", 2);

        if (parts.length < 2 || parts[1].isBlank()) {
            return null;
        }

        return parts[1].trim();
    }
}
