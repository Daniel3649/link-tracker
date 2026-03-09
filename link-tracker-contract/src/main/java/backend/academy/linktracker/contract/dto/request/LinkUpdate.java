package backend.academy.linktracker.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;

public record LinkUpdate(
    @NotNull
    @Positive
    Long id,

    @NotNull
    URI url,

    @NotBlank
    String description,

    @NotNull
    @NotEmpty
    List<@NotNull @Positive Long> tgChatIds
) {}
