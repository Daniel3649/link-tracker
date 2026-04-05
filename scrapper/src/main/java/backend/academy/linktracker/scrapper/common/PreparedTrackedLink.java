package backend.academy.linktracker.scrapper.common;

public record PreparedTrackedLink(ParsedLink parsedLink, PreparedTrackingState trackingState) {}
