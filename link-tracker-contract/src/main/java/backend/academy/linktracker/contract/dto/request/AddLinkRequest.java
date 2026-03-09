package backend.academy.linktracker.contract.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;

public record AddLinkRequest(
        @NotNull URI link,

        @NotNull Set<@NotBlank String> tags,

        @NotNull List<@NotBlank String> filters) {
    @JsonCreator
    public AddLinkRequest {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        filters = filters == null ? List.of() : List.copyOf(filters);
    }
}
