package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "github_tracking_state")
@Getter
@Setter
@NoArgsConstructor
public class GitHubTrackingStateEntity {
    @Id
    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "link_id", nullable = false)
    private TrackedLinkEntity trackedLink;

    @Column(name = "last_activity_id")
    private Long lastActivityId;
}
