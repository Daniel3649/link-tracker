package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.service.LinkUpdateNotificationService;
import backend.academy.linktracker.contract.dto.request.LinkUpdate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
public class LinkUpdateController {
    private final LinkUpdateNotificationService notificationService;

    @PostMapping
    public ResponseEntity<Void> handleUpdate(@Valid @RequestBody LinkUpdate linkUpdate) {
        notificationService.process(linkUpdate);
        return ResponseEntity.ok().build();
    }
}
