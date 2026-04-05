package backend.academy.linktracker.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record TextNotification(
        @NotBlank String message, @NotNull @NotEmpty List<@NotNull @Positive Long> tgChatIds) {}
