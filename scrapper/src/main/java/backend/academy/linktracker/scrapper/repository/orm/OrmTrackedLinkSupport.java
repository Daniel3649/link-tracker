package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.models.link.TrackedLink;
import backend.academy.linktracker.scrapper.models.link.resourcekey.GitHubRepositoryKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;
import backend.academy.linktracker.scrapper.models.link.resourcekey.StackOverflowQuestionKey;
import backend.academy.linktracker.scrapper.repository.orm.entity.LinkTypeEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;

final class OrmTrackedLinkSupport {
    private OrmTrackedLinkSupport() {}

    static TrackedLink toDomain(TrackedLinkEntity entity) {
        ResourceKey resourceKey = switch (entity.getLinkType()) {
            case GITHUB -> new GitHubRepositoryKey(entity.getGithubOwner(), entity.getGithubRepo());
            case STACKOVERFLOW -> new StackOverflowQuestionKey(entity.getStackoverflowQuestionId());
        };

        return new TrackedLink(entity.getId(), entity.getUrl(), resourceKey);
    }

    static TrackedLinkEntity toNewEntity(TrackedLink trackedLink) {
        TrackedLinkEntity entity = new TrackedLinkEntity();
        fillEntity(entity, trackedLink);
        return entity;
    }

    static void fillEntity(TrackedLinkEntity entity, TrackedLink trackedLink) {
        entity.setUrl(trackedLink.getUrl());

        switch (trackedLink.getResourceKey()) {
            case GitHubRepositoryKey gitHubRepositoryKey -> {
                entity.setLinkType(LinkTypeEntity.GITHUB);
                entity.setGithubOwner(gitHubRepositoryKey.owner());
                entity.setGithubRepo(gitHubRepositoryKey.repo());
                entity.setStackoverflowQuestionId(null);
            }
            case StackOverflowQuestionKey stackOverflowQuestionKey -> {
                entity.setLinkType(LinkTypeEntity.STACKOVERFLOW);
                entity.setGithubOwner(null);
                entity.setGithubRepo(null);
                entity.setStackoverflowQuestionId(stackOverflowQuestionKey.questionId());
            }
        }
    }
}
