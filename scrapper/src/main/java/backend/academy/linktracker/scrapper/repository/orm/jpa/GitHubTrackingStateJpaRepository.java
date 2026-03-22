package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.GitHubTrackingStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GitHubTrackingStateJpaRepository extends JpaRepository<GitHubTrackingStateEntity, Long> {}
