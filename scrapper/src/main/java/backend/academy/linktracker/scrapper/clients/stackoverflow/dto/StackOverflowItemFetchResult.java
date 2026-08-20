package backend.academy.linktracker.scrapper.clients.stackoverflow.dto;

public record StackOverflowItemFetchResult<T>(T item, Integer backoffSeconds) {}
