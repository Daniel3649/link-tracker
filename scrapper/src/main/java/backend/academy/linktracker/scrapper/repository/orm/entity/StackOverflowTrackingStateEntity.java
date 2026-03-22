package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stackoverflow_tracking_state")
@Getter
@Setter
@NoArgsConstructor
public class StackOverflowTrackingStateEntity {
    @Id
    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "link_id", nullable = false)
    private TrackedLinkEntity trackedLink;

    @Column(name = "last_creation_date_epoch_sec", nullable = false)
    private long lastCreationDateEpochSec;

    @Column(name = "last_event_key")
    private String lastEventKey;

    @Column(name = "next_check_at")
    private Instant nextCheckAt;

    @Column(name = "last_question_activity_date_epoch_sec", nullable = false)
    private long lastQuestionActivityDateEpochSec;
}
