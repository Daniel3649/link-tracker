package backend.academy.linktracker.scrapper.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SpringLiquibase.class)
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LiquibaseConfiguration {
    @Bean
    SpringLiquibase liquibase(DataSource dataSource, @Value("${spring.liquibase.change-log}") String changeLog) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(resolveChangeLogPath(changeLog));
        return liquibase;
    }

    private String resolveChangeLogPath(String changeLog) {
        if (!changeLog.startsWith("file:")) {
            return changeLog;
        }

        String configuredPath = changeLog.substring("file:".length());
        if (Files.exists(Path.of(configuredPath))) {
            return changeLog;
        }

        String[] fallbackPaths = {"./migrations/master.xml", "../migrations/master.xml"};
        for (String fallbackPath : fallbackPaths) {
            if (Files.exists(Path.of(fallbackPath))) {
                return "file:" + fallbackPath;
            }
        }

        return changeLog;
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerFactoryDependsOnLiquibase() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                beanFactory.getBeanDefinition("entityManagerFactory").setDependsOn("liquibase");
            }
        };
    }
}
