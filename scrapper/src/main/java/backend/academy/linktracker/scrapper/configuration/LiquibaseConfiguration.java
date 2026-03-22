package backend.academy.linktracker.scrapper.configuration;

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
    SpringLiquibase liquibase(
            DataSource dataSource,
            @Value("${spring.liquibase.change-log}") String changeLog) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        return liquibase;
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
