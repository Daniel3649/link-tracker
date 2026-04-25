package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bot")
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class BotProperties {
    @NotBlank
    private String baseUrl = "http://localhost:8080";

    @NotBlank
    private String grpcAddress = "localhost:9090";

    private TransportType transport = TransportType.HTTP;
}
