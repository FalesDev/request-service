package co.com.pragma.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "RegisterApplicationRequestDto", description = "Request body for registering a loan application")
public record RegisterApplicationRequestDto(
        @Schema(description = "Requested loan amount", example = "10000.0")
        @NotNull(message = "Amount is required")
        Double amount,

        @Schema(description = "Loan term in months", example = "12")
        @NotNull(message = "Term is required")
        @Min(value = 1, message = "Term must be at least 1 month")
        Integer term,

        @Schema(description = "Applicant's identification document", example = "12345678")
        @NotBlank(message = "IdDocument is required")
        String idDocument,

        @Schema(description = "Loan type identifier", example = "112fb229-db20-4800-93a4-f76e822a495b")
        @NotNull(message = "IdLoanType is required")
        UUID idLoanType
) {
}
