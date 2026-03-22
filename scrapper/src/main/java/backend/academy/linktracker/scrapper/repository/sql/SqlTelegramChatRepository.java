package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.models.chat.TelegramChat;
import backend.academy.linktracker.scrapper.repository.TelegramChatRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RequiredArgsConstructor
public class SqlTelegramChatRepository implements TelegramChatRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<TelegramChat> findByChatId(Long chatId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("chatId", chatId);

        return jdbcTemplate
                .query(
                        "select chat_id from telegram_chat where chat_id = :chatId",
                        parameters,
                        (resultSet, rowNum) -> new TelegramChat(resultSet.getLong("chat_id")))
                .stream()
                .findFirst();
    }

    @Override
    public boolean saveIfAbsent(TelegramChat telegramChat) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("chatId", telegramChat.id());

        return jdbcTemplate.update("""
                        insert into telegram_chat (chat_id)
                        values (:chatId)
                        on conflict (chat_id) do nothing
                        """, parameters) > 0;
    }

    @Override
    public TelegramChat save(TelegramChat telegramChat) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("chatId", telegramChat.id());

        jdbcTemplate.update("""
                insert into telegram_chat (chat_id)
                values (:chatId)
                on conflict (chat_id) do nothing
                """, parameters);

        return telegramChat;
    }

    @Override
    public boolean existsByChatId(Long chatId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("chatId", chatId);

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select exists(select 1 from telegram_chat where chat_id = :chatId)", parameters, Boolean.class));
    }

    @Override
    public void deleteByChatId(Long chatId) {
        jdbcTemplate.update(
                "delete from telegram_chat where chat_id = :chatId", new MapSqlParameterSource("chatId", chatId));
    }

    @Override
    public void clear() {
        jdbcTemplate.getJdbcTemplate().execute("truncate table telegram_chat cascade");
    }
}
