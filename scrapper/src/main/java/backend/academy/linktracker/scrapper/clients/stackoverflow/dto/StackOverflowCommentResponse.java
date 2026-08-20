package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StackOverflowCommentResponse(
        @JsonProperty("comment_id") Long commentId,
        @JsonProperty("creation_date") Long creationDateEpochSec,
        String body,
        StackOverflowOwnerResponse owner) {}
