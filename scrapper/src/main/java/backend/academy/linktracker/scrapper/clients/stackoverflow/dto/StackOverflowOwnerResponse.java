package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowOwnerResponse(
        @JsonProperty("display_name") String displayName) {}
