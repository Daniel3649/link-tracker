package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StackOverflowQuestionTimelineEventResponse(
        @JsonProperty("creation_date") Long creationDateEpochSec,

        @JsonProperty("timeline_type") String timelineType,

        @JsonProperty("question_id") Long questionId,

        @JsonProperty("post_id") Long postId,

        @JsonProperty("comment_id") Long commentId,

        @JsonProperty("revision_guid") String revisionGuid) {}
