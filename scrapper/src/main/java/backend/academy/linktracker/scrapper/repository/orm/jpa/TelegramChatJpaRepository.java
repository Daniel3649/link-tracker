package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.TelegramChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramChatJpaRepository extends JpaRepository<TelegramChatEntity, Long> {}
