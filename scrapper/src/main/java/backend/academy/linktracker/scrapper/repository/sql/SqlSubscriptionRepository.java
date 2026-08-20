package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.domains.chat.TelegramChat;
import backend.academy.linktracker.scrapper.domains.link.TrackedLink;
import backend.academy.linktracker.scrapper.domains.subscription.Subscription;
import backend.academy.linktracker.scrapper.exception.subscription.SubscriptionNotFoundException;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@RequiredArgsConstructor
public class SqlSubscriptionRepository implements SubscriptionRepository {
    private static final String SUBSCRIPTION_SELECT = """
            select
                s.id as subscription_id,
                tl.id,
                tl.url,
                tl.link_type,
                tl.github_owner,
                tl.github_repo,
                tl.stackoverflow_question_id,
                s.chat_id
            from subscription s
            join tracked_link tl on tl.id = s.link_id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<Subscription> saveIfAbsent(Subscription subscription) {
        MapSqlParameterSource parameters = parameters(subscription.getTrackedLink(), subscription.getTelegramChat());

        List<Long> insertedIds = jdbcTemplate.query("""
                insert into subscription (chat_id, link_id)
                values (:chatId, :linkId)
                on conflict (chat_id, link_id) do nothing
                returning id
                """, parameters, (resultSet, rowNum) -> resultSet.getLong("id"));

        if (insertedIds.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Subscription(
                insertedIds.getFirst(), subscription.getTrackedLink(), subscription.getTelegramChat()));
    }

    @Override
    public boolean existsByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        MapSqlParameterSource parameters = parameters(trackedLink, telegramChat);

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists(
                    select 1
                    from subscription
                    where link_id = :linkId and chat_id = :chatId
                )
                """, parameters, Boolean.class));
    }

    @Override
    public Subscription save(Subscription subscription) {
        if (subscription.getId() != null) {
            MapSqlParameterSource parameters = parameters(subscription.getTrackedLink(), subscription.getTelegramChat())
                    .addValue("id", subscription.getId());

            int updated = jdbcTemplate.update("""
                update subscription
                set link_id = :linkId,
                    chat_id = :chatId
                where id = :id
                """, parameters);

            if (updated == 0) {
                throw new SubscriptionNotFoundException("Subscription not found with id: " + subscription.getId());
            }

            return subscription;
        }

        return findByTrackedLinkAndTelegramChat(subscription.getTrackedLink(), subscription.getTelegramChat())
                .orElseGet(() -> {
                    MapSqlParameterSource parameters =
                            parameters(subscription.getTrackedLink(), subscription.getTelegramChat());

                    KeyHolder keyHolder = new GeneratedKeyHolder();

                    jdbcTemplate.update("""
                insert into subscription (link_id, chat_id)
                values (:linkId, :chatId)
                """, parameters, keyHolder, new String[] {"id"});

                    Number key = keyHolder.getKey();
                    if (key == null) {
                        throw new IllegalStateException("Failed to generate id for subscription");
                    }

                    return new Subscription(
                            key.longValue(), subscription.getTrackedLink(), subscription.getTelegramChat());
                });
    }

    @Override
    public boolean existsByTrackedLink(TrackedLink trackedLink) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists(select 1 from subscription where link_id = :linkId)",
                new MapSqlParameterSource("linkId", trackedLink.getId()),
                Boolean.class));
    }

    @Override
    public Optional<Subscription> findByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        MapSqlParameterSource parameters = parameters(trackedLink, telegramChat);

        return jdbcTemplate
                .query(
                        SUBSCRIPTION_SELECT + " where s.link_id = :linkId and s.chat_id = :chatId",
                        parameters,
                        (resultSet, rowNum) -> mapSubscription(resultSet))
                .stream()
                .findFirst();
    }

    @Override
    public void deleteByTrackedLinkAndTelegramChat(TrackedLink trackedLink, TelegramChat telegramChat) {
        jdbcTemplate.update(
                "delete from subscription where link_id = :linkId and chat_id = :chatId",
                parameters(trackedLink, telegramChat));
    }

    @Override
    public List<Subscription> findAllByTelegramChatId(Long chatId) {
        return jdbcTemplate.query(
                SUBSCRIPTION_SELECT + " where s.chat_id = :chatId order by s.id",
                new MapSqlParameterSource("chatId", chatId),
                (resultSet, rowNum) -> mapSubscription(resultSet));
    }

    @Override
    public List<Subscription> findAllByTrackedLink(TrackedLink trackedLink) {
        return jdbcTemplate.query(
                SUBSCRIPTION_SELECT + " where s.link_id = :linkId order by s.id",
                new MapSqlParameterSource("linkId", trackedLink.getId()),
                (resultSet, rowNum) -> mapSubscription(resultSet));
    }

    @Override
    public List<Long> findAllChatIdsByTrackedLinkId(Long trackedLinkId) {
        return jdbcTemplate.query(
                "select s.chat_id from subscription s where s.link_id = :linkId order by s.id",
                new MapSqlParameterSource("linkId", trackedLinkId),
                (resultSet, rowNum) -> resultSet.getLong("chat_id"));
    }

    @Override
    public void clear() {
        jdbcTemplate.getJdbcTemplate().execute("truncate table subscription cascade");
    }

    private MapSqlParameterSource parameters(TrackedLink trackedLink, TelegramChat telegramChat) {
        return new MapSqlParameterSource()
                .addValue("linkId", trackedLink.getId())
                .addValue("chatId", telegramChat.id());
    }

    private Subscription mapSubscription(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        TrackedLink trackedLink = SqlTrackedLinkSupport.mapTrackedLink(resultSet);
        TelegramChat telegramChat = new TelegramChat(resultSet.getLong("chat_id"));
        return new Subscription(resultSet.getLong("subscription_id"), trackedLink, telegramChat);
    }
}
