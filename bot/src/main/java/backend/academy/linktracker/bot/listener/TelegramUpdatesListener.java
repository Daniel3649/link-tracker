package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.service.UpdateService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUpdatesListener implements UpdatesListener {
    private final TelegramBot bot;
    private final UpdateService updateService;

    @PostConstruct
    public void start() {
        bot.setUpdatesListener(this);
    }

    @PreDestroy
    public void stop() {
        bot.removeGetUpdatesListener();
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(updateService::handleEvent);
        return CONFIRMED_UPDATES_ALL;
    }
}
