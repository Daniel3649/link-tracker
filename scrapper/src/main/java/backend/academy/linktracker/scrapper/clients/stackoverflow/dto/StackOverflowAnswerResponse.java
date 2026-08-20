package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StackOverflowAnswerResponse(
        @JsonProperty("answer_id") Long answerId,
        @JsonProperty("creation_date") Long creationDateEpochSec,
        String body,
        StackOverflowOwnerResponse owner) {}
