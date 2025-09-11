package co.com.pragma.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "IdApplication is required")
        UUID idApplication,
        @NotNull(message = "Status is required")
        String status
) {
}
