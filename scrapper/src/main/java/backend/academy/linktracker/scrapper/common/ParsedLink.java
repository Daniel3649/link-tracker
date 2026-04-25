package backend.academy.linktracker.scrapper.common;

import backend.academy.linktracker.scrapper.domains.link.resourcekey.ResourceKey;

public record ParsedLink(String url, ResourceKey resourceKey) {}
