package backend.academy.linktracker.scrapper.service;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=SQL")
class SubscriptionServiceTransactionSqlIntegrationTest extends SubscriptionServiceTransactionIntegrationTest {}
