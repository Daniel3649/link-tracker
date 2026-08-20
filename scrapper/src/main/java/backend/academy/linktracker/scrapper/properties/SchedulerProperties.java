package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.scheduler")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@Validated
public class SchedulerProperties {
    private Duration linkCheckDelayMs = Duration.ofSeconds(60);

    @Min(50)
    @Max(500)
    private int linkCheckBatchSize = 100;

    @Min(1)
    @Max(16)
    private int linkCheckParallelism = 4;
}
