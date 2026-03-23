package backend.academy.linktracker.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;

public record RemoveTagRequest(@NotNull URI link, @NotBlank String tag) {}
