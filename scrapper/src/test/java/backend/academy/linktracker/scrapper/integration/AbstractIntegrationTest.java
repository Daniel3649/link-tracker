package backend.academy.linktracker.scrapper.integration;

import backend.academy.linktracker.scrapper.ScrapperApplication;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = ScrapperApplication.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractIntegrationTest extends TestcontainersConfiguration {}
