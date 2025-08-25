package co.com.pragma.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterApplicationRequestDto(
        @NotNull(message = "Amount is required")
        Double amount,
        @NotNull(message = "Term is required")
        Integer term,
        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "IdDocument is required")
        String idDocument,
        @NotNull(message = "IdLoanType is required")
        UUID idLoanType
) {
}
