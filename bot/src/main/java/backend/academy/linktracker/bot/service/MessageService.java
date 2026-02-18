package backend.academy.linktracker.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        Objects.requireNonNull(key, "Message key is null");
        return messageSource.getMessage(key, args, Locale.US);
    }
}
