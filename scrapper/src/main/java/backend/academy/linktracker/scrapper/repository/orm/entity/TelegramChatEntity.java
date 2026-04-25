package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "telegram_chat")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelegramChatEntity {
    @Id
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    public TelegramChatEntity(Long chatId) {
        this.chatId = chatId;
    }
}
