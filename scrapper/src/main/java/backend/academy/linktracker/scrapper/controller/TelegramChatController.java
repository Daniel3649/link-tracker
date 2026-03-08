package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.service.TelegramChatService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class TelegramChatController {
    private final TelegramChatService telegramChatService;

    @PostMapping("/tg-chat/{id}")
    public ResponseEntity<Void> registerChat(@PathVariable("id") @Positive long id) {
        telegramChatService.registerChat(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tg-chat/{id}")
    public ResponseEntity<Void> unregisterChat(@PathVariable("id") @Positive long id) {
        telegramChatService.unregisterChat(id);
        return ResponseEntity.ok().build();
    }
}
