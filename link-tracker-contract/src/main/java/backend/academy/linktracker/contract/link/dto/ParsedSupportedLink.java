package backend.academy.linktracker.contract.link.dto;

import java.net.URI;

public sealed interface ParsedSupportedLink permits ParsedGitHubRepositoryLink, ParsedStackOverflowQuestionLink {
    URI uri();
    SupportedLinkKind kind();
}
