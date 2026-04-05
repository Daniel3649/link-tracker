package backend.academy.linktracker.scrapper.service;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=ORM")
class SubscriptionConcurrencyOrmIntegrationTest extends SubscriptionConcurrencyIntegrationTest {}
