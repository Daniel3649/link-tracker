package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.link.parser.GitHubRepositoryLinkParser;
import backend.academy.linktracker.scrapper.link.parser.StackOverflowQuestionLinkParser;
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
}
