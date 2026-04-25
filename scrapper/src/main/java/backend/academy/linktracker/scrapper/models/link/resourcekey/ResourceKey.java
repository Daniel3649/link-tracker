package backend.academy.linktracker.scrapper.models.link.resourcekey;

public sealed interface ResourceKey permits GitHubRepositoryKey, StackOverflowQuestionKey {}
