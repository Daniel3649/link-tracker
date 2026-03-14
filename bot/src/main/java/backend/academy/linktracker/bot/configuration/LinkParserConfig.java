package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.contract.link.common.ParsedSupportedLink;
import backend.academy.linktracker.contract.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.contract.link.parser.LinkParser;
import backend.academy.linktracker.contract.link.parser.StackOverflowQuestionLinkParser;
import backend.academy.linktracker.contract.link.parser.SupportedLinkParser;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public SupportedLinkParser supportedLinkParser(List<LinkParser<? extends ParsedSupportedLink>> parsers) {
        return new SupportedLinkParser(parsers);
    }
}
