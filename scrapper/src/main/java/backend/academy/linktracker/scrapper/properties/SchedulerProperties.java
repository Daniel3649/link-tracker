package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scheduler")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class SchedulerProperties {
    private Duration linkCheckDelayMs = Duration.ofSeconds(60);

    @Min(1)
    private int linkCheckBatchSize = 100;
}
