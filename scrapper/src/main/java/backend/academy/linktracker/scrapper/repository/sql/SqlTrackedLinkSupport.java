package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

final class SqlTrackedLinkSupport {
    static final String TRACKED_LINK_COLUMNS =
            "id, url, link_type, github_owner, github_repo, stackoverflow_question_id";

    private SqlTrackedLinkSupport() {}

    static MapSqlParameterSource trackedLinkParams(TrackedLink trackedLink) {
        MapSqlParameterSource parameters = resourceKeyParams(trackedLink.getResourceKey());
        parameters.addValue("id", trackedLink.getId());
        parameters.addValue("url", trackedLink.getUrl());
        return parameters;
    }

    static MapSqlParameterSource resourceKeyParams(ResourceKey resourceKey) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        switch (resourceKey) {
            case GitHubRepositoryKey gitHubRepositoryKey -> {
                parameters.addValue("linkType", "GITHUB");
                parameters.addValue("githubOwner", gitHubRepositoryKey.owner());
                parameters.addValue("githubRepo", gitHubRepositoryKey.repo());
                parameters.addValue("stackoverflowQuestionId", null);
            }
            case StackOverflowQuestionKey stackOverflowQuestionKey -> {
                parameters.addValue("linkType", "STACKOVERFLOW");
                parameters.addValue("githubOwner", null);
                parameters.addValue("githubRepo", null);
                parameters.addValue("stackoverflowQuestionId", stackOverflowQuestionKey.questionId());
            }
        }

        return parameters;
    }

    static String resourceKeyPredicate(ResourceKey resourceKey) {
        return switch (resourceKey) {
            case GitHubRepositoryKey ignored ->
                "link_type = 'GITHUB' and lower(github_owner) = lower(:githubOwner)"
                        + " and lower(github_repo) = lower(:githubRepo)";
            case StackOverflowQuestionKey ignored ->
                "link_type = 'STACKOVERFLOW' and stackoverflow_question_id = :stackoverflowQuestionId";
        };
    }

    static TrackedLink mapTrackedLink(ResultSet resultSet) throws SQLException {
        Long id = resultSet.getLong("id");
        String url = resultSet.getString("url");
        String linkType = resultSet.getString("link_type");

        ResourceKey resourceKey =
                switch (linkType) {
                    case "GITHUB" ->
                        new GitHubRepositoryKey(
                                resultSet.getString("github_owner"), resultSet.getString("github_repo"));
                    case "STACKOVERFLOW" ->
                        new StackOverflowQuestionKey(resultSet.getLong("stackoverflow_question_id"));
                    default -> throw new IllegalStateException("Unsupported link_type: " + linkType);
                };

        return new TrackedLink(id, url, resourceKey);
    }
}
