package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.service.TextNotificationService;
import backend.academy.linktracker.contract.dto.request.TextNotification;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications/text")
@RequiredArgsConstructor
public class TextNotificationController {
    private final TextNotificationService textNotificationService;

    @PostMapping
    public ResponseEntity<Void> handleNotification(@Valid @RequestBody TextNotification notification) {
        textNotificationService.sendNotification(notification);
        return ResponseEntity.ok().build();
    }
}
