package co.com.pragma.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record RegisterApplicationRequestDto(
        @NotNull(message = "Amount is required")
        Double amount,
        @NotNull(message = "Term is required")
        Integer term,
        @Pattern(
                regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Email should be valid"
        )
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "IdDocument is required")
        String idDocument,
        @NotNull(message = "IdLoanType is required")
        UUID idLoanType
) {
}
