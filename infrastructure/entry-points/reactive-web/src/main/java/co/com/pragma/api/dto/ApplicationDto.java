package co.com.pragma.api.dto;

import java.util.UUID;

public record ApplicationDto (
        UUID id,
        Double amount,
        Integer term,
        String email,
        String idDocument,
        UUID idStatus,
        UUID idLoanType,
        UUID idUser
){
}
