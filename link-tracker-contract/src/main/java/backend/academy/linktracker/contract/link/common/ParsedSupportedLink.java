package backend.academy.linktracker.contract.link.common;

import java.net.URI;

public sealed interface ParsedSupportedLink permits ParsedGitHubRepositoryLink, ParsedStackOverflowQuestionLink {
    URI uri();
}
