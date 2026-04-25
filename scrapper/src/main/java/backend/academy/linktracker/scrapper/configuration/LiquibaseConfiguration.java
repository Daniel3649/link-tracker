package backend.academy.linktracker.scrapper.configuration;

import java.util.List;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class LiquibaseConfiguration {
    private static final List<String> FALLBACK_CHANGE_LOGS = List.of(
            "file:./migrations/master.xml", "file:../migrations/master.xml", "classpath:/migrations/master.xml");

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(resolveChangeLog());
        liquibase.setShouldRun(environment.getProperty("spring.liquibase.enabled", Boolean.class, true));
        return liquibase;
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnLiquibase() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn("liquibase");
            }
        };
    }

    private String resolveChangeLog() {
        String configuredChangeLog = environment.getProperty("spring.liquibase.change-log");
        if (StringUtils.hasText(configuredChangeLog)
                && resourceLoader.getResource(configuredChangeLog).exists()) {
            return configuredChangeLog;
        }

        return FALLBACK_CHANGE_LOGS.stream()
                .filter(candidate -> resourceLoader.getResource(candidate).exists())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Liquibase changelog was not found"));
    }
}
