package backend.academy.linktracker.bot.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scrapper")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class ScrapperProperties {
    @NotBlank
    private String baseUrl = "http://localhost:8081";

    @NotBlank
    private String grpcAddress = "localhost:9091";

    private TransportType transport = TransportType.HTTP;
}
