package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.contract.dto.request.AddLinkRequest;
import backend.academy.linktracker.contract.dto.request.RemoveLinkRequest;
import backend.academy.linktracker.contract.dto.response.LinkResponse;
import backend.academy.linktracker.contract.dto.response.ListLinksResponse;
import backend.academy.linktracker.scrapper.service.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    @GetMapping("/links")
    public ResponseEntity<ListLinksResponse> getSubscription(@RequestHeader("Tg-Chat-Id") @Positive long chatId) {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(chatId));
    }

    @PostMapping("/links")
    public ResponseEntity<LinkResponse> addSubscription(
            @RequestHeader("Tg-Chat-Id") @Positive long chatId, @Valid @RequestBody AddLinkRequest request) {
        return ResponseEntity.ok(subscriptionService.addSubscription(chatId, request));
    }

    @DeleteMapping("/links")
    public ResponseEntity<LinkResponse> removeSubscription(
            @RequestHeader("Tg-Chat-Id") @Positive long chatId, @Valid @RequestBody RemoveLinkRequest request) {
        return ResponseEntity.ok(subscriptionService.removeSubscription(chatId, request));
    }
}
