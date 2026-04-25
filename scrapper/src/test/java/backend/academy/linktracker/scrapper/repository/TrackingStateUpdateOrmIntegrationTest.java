package backend.academy.linktracker.scrapper.repository;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=ORM")
class TrackingStateUpdateOrmIntegrationTest extends TrackingStateUpdateIntegrationTest {}
