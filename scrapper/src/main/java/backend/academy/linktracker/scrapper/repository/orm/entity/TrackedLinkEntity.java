package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tracked_link")
@Getter
@Setter
@NoArgsConstructor
public class TrackedLinkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 32)
    private LinkTypeEntity linkType;

    @Column(name = "github_owner")
    private String githubOwner;

    @Column(name = "github_repo")
    private String githubRepo;

    @Column(name = "stackoverflow_question_id")
    private Long stackoverflowQuestionId;
}
