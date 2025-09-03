package co.com.pragma.model.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ValidatedUser {
    private String email;
    private String idDocument;
    private String role;
}
