package co.com.pragma.model.auth;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserFound {
    private UUID idUser;
    private String firstName;
    private String lastName;
    private String email;
    private String idDocument;
    private Double baseSalary;
}
