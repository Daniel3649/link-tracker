package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscription_tag")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionTagEntity {
    @EmbeddedId
    private SubscriptionTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("subscriptionId")
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionEntity subscription;

    public SubscriptionTagEntity(SubscriptionEntity subscription, String tag) {
        this.subscription = subscription;
        this.id = new SubscriptionTagId(subscription.getId(), tag);
    }

    public String getTag() {
        return this.id.getTag();
    }
}
