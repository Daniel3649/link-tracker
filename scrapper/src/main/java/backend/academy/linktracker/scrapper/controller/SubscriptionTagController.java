package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.contract.dto.request.AddTagRequest;
import backend.academy.linktracker.contract.dto.request.RemoveTagRequest;
import backend.academy.linktracker.contract.dto.request.UpdateTagRequest;
import backend.academy.linktracker.contract.dto.response.ListTagsResponse;
import backend.academy.linktracker.contract.dto.response.TagResponse;
import backend.academy.linktracker.scrapper.service.SubscriptionTagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class SubscriptionTagController {
    private final SubscriptionTagService subscriptionTagService;

    @GetMapping("/tags")
    public ResponseEntity<ListTagsResponse> getTags(
            @RequestHeader("Tg-Chat-Id") @Positive long chatId, @RequestParam URI link) {
        return ResponseEntity.ok(subscriptionTagService.getTags(chatId, link));
    }

    @PostMapping("/tags")
    public ResponseEntity<TagResponse> addTag(
            @RequestHeader("Tg-Chat-Id") @Positive long chatId, @Valid @RequestBody AddTagRequest request) {
        return ResponseEntity.ok(subscriptionTagService.addTag(chatId, request));
    }

    @PutMapping("/tags")
    public ResponseEntity<TagResponse> updateTag(
            @RequestHeader("Tg-Chat-Id") @Positive long chatId, @Valid @RequestBody UpdateTagRequest request) {
        return ResponseEntity.ok(subscriptionTagService.updateTag(chatId, request));
    }

    @DeleteMapping("/tags")
    public ResponseEntity<TagResponse> removeTag(
            @RequestHeader("Tg-Chat-Id") @Positive long chatId, @Valid @RequestBody RemoveTagRequest request) {
        return ResponseEntity.ok(subscriptionTagService.removeTag(chatId, request));
    }
}
