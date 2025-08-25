package co.com.pragma.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("applications")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ApplicationEntity {

    @Id
    private UUID id;
    private Double amount;
    private Integer term;
    private String email;
    private String idDocument;
    private UUID idStatus;
    private UUID idLoanType;
}
