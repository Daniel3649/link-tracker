package backend.academy.linktracker.scrapper.handlers.link;

import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;

public record ParsedLink(
    String url,
    ResourceKey resourceKey,
    ResourceType resourceType) {
}
