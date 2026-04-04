package backend.academy.linktracker.scrapper.controller;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.database.access-type=SQL")
class ScrapperFullChainSqlIntegrationTest extends ScrapperFullChainIntegrationTest {}
