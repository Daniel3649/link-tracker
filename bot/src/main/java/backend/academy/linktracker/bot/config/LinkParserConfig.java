package backend.academy.linktracker.bot.config;

import backend.academy.linktracker.contract.link.dto.ParsedSupportedLink;
import backend.academy.linktracker.contract.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.contract.link.parser.LinkParser;
import backend.academy.linktracker.contract.link.parser.StackOverflowQuestionLinkParser;
import backend.academy.linktracker.contract.link.parser.SupportedLinkParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class LinkParserConfig {

    @Bean
    public GitHubRepositoryLinkParser gitHubRepositoryLinkParser() {
        return new GitHubRepositoryLinkParser();
    }

    @Bean
    public StackOverflowQuestionLinkParser stackOverflowQuestionLinkParser() {
        return new StackOverflowQuestionLinkParser();
    }

    @Bean
    public SupportedLinkParser supportedLinkParser(
        GitHubRepositoryLinkParser gitHubRepositoryLinkParser,
        StackOverflowQuestionLinkParser stackOverflowQuestionLinkParser
    ) {
        List<LinkParser<? extends ParsedSupportedLink>> parsers = List.of(
            gitHubRepositoryLinkParser,
            stackOverflowQuestionLinkParser
        );

        return new SupportedLinkParser(parsers);
    }
}
