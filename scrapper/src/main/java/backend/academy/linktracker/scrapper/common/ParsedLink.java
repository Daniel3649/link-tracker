package backend.academy.linktracker.scrapper.common;

import backend.academy.linktracker.scrapper.models.link.resourcekey.ResourceKey;

public record ParsedLink(String url, ResourceKey resourceKey) {}
