package backend.academy.linktracker.scrapper.integration;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=SQL")
class TrackingStateUpdateSqlIntegrationTest extends TrackingStateUpdateIntegrationTest {}
