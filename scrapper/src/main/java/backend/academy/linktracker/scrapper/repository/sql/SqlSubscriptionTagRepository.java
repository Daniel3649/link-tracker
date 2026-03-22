package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.models.subscription.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionTagRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@RequiredArgsConstructor
public class SqlSubscriptionTagRepository implements SubscriptionTagRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void addTags(Subscription subscription, Set<String> subscriptionTags) {
        if (subscriptionTags == null || subscriptionTags.isEmpty()) {
            return;
        }

        SqlParameterSource[] batchParameters = subscriptionTags.stream()
                .distinct()
                .map(tag -> new MapSqlParameterSource()
                        .addValue("subscriptionId", subscription.getId())
                        .addValue("tag", tag))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(
                """
                insert into subscription_tag (subscription_id, tag)
                values (:subscriptionId, :tag)
                on conflict (subscription_id, tag) do nothing
                """,
                batchParameters);
    }

    @Override
    public void deleteAllBySubscription(Subscription subscription) {
        jdbcTemplate.update(
                "delete from subscription_tag where subscription_id = :subscriptionId",
                new MapSqlParameterSource("subscriptionId", subscription.getId()));
    }

    @Override
    public Set<String> findAllBySubscription(Subscription subscription) {
        return new LinkedHashSet<>(jdbcTemplate.query(
                "select tag from subscription_tag where subscription_id = :subscriptionId order by tag",
                new MapSqlParameterSource("subscriptionId", subscription.getId()),
                (resultSet, rowNum) -> resultSet.getString("tag")));
    }

    @Override
    public void clear() {
        jdbcTemplate.getJdbcTemplate().execute("truncate table subscription_tag cascade");
    }
}
