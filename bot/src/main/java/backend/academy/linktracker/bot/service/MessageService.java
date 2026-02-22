package backend.academy.linktracker.bot.service;

import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        Objects.requireNonNull(key, "key cannot be null");
        return messageSource.getMessage(key, args, Locale.US);
    }
}
