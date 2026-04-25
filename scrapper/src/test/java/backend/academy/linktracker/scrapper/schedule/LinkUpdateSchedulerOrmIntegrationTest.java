package backend.academy.linktracker.scrapper.schedule;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=ORM")
class LinkUpdateSchedulerOrmIntegrationTest extends LinkUpdateSchedulerIntegrationTest {}
