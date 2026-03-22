package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionTagId implements Serializable {
    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @Column(name = "tag", nullable = false)
    private String tag;
}
