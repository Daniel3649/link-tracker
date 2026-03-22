package backend.academy.linktracker.scrapper.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

abstract class DatabaseMigrationsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyLiquibaseMigrationsOnCleanDatabaseStartup() {
        Integer trackedLinkTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.tables
                where table_schema = 'public' and table_name = 'tracked_link'
                """,
                Integer.class);

        Integer changelogEntries = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog", Integer.class);

        assertThat(trackedLinkTableCount).isEqualTo(1);
        assertThat(changelogEntries).isNotNull().isGreaterThan(0);
    }
}
