package backend.academy.linktracker.scrapper.schedule;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=SQL")
class LinkUpdateSchedulerSqlIntegrationTest extends LinkUpdateSchedulerIntegrationTest {}
