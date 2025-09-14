package co.com.pragma.model.auth;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ValidatedUser {
    private UUID idUser;
    private String email;
    private String idDocument;
    private Double baseSalary;
    private String role;
}
