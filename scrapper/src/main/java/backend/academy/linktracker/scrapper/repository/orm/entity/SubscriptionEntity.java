package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "subscription",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_subscription_chat_link", columnNames = {"chat_id", "link_id"})
        })
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false)
    private TrackedLinkEntity trackedLink;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private TelegramChatEntity telegramChat;

    @Column(name = "chat_id", insertable = false, updatable = false, nullable = false)
    private Long chatId;

    @Column(name = "link_id", insertable = false, updatable = false, nullable = false)
    private Long linkId;
}
