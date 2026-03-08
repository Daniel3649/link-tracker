package backend.academy.linktracker.scrapper.clients.stackoverflow;

import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowApiResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionFetchResult;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowQuestionTimelineEventResponse;
import backend.academy.linktracker.scrapper.clients.stackoverflow.dto.StackOverflowTimelineFetchResult;
import backend.academy.linktracker.scrapper.exception.UpstreamException;
import backend.academy.linktracker.scrapper.exception.client.RepositoryPollingException;
import backend.academy.linktracker.scrapper.links.stackoverflow.StackOverflowLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.models.link.trackingstate.cursor.StackOverflowTimelineCursor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class StackOverflowClient {
    private static final ParameterizedTypeReference<StackOverflowApiResponse<StackOverflowQuestionResponse>>
        QUESTION_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<StackOverflowApiResponse<StackOverflowQuestionTimelineEventResponse>>
        TIMELINE_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final RestClient stackOverflowRestClient;

    @Value("${app.stackoverflow.site:stackoverflow}")
    private String site;

    @Value("${app.stackoverflow.key:}")
    private String key;


    public StackOverflowQuestionFetchResult fetchQuestion(StackOverflowQuestionKey questionKey) {
        try {
            StackOverflowApiResponse<StackOverflowQuestionResponse> response = stackOverflowRestClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path("/questions/{id}")
                        .queryParam("site", site);

                    if (StringUtils.hasText(key)) {
                        builder.queryParam("key", key);
                    }

                    return builder.build(questionKey.questionId());
                })
                .retrieve()
                .body(QUESTION_RESPONSE_TYPE);

            validateResponse(response, "question " + questionKey.questionId());

            StackOverflowQuestionResponse question =
                response.items() == null || response.items().isEmpty()
                    ? null
                    : response.items().getFirst();

            return new StackOverflowQuestionFetchResult(question, response.backoff());
        } catch (RestClientException e) {
            throw new RepositoryPollingException(
                "Failed to call StackOverflow API for question %s".formatted(questionKey.questionId()),
                e
            );
        }
    }

    public StackOverflowTimelineFetchResult fetchQuestionTimeline(
        StackOverflowQuestionKey questionKey,
        int pageSize
    ) {
        try {
            StackOverflowApiResponse<StackOverflowQuestionTimelineEventResponse> response = stackOverflowRestClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path("/questions/{id}/timeline")
                        .queryParam("site", site)
                        .queryParam("pagesize", pageSize);

                    if (StringUtils.hasText(key)) {
                        builder.queryParam("key", key);
                    }

                    return builder.build(questionKey.questionId());
                })
                .retrieve()
                .body(TIMELINE_RESPONSE_TYPE);

            validateResponse(response, "question timeline " + questionKey.questionId());

            List<StackOverflowQuestionTimelineEventResponse> events =
                response.items() == null ? List.of() : response.items();

            return new StackOverflowTimelineFetchResult(events, response.backoff());
        } catch (RestClientException e) {
            throw new RepositoryPollingException(
                "Failed to call StackOverflow timeline API for question %s".formatted(questionKey.questionId()),
                e
            );
        }
    }

    private void validateResponse(StackOverflowApiResponse<?> response, String target) {
        if (response == null) {
            throw new RepositoryPollingException("Empty StackOverflow API response for " + target);
        }

        if (response.errorId() != null) {
            throw new RepositoryPollingException(
                "StackOverflow API error for %s: %s (%s)"
                    .formatted(target, response.errorMessage(), response.errorName())
            );
        }
    }
}
