package co.com.pragma.api.mapper;

import co.com.pragma.api.dto.ApplicationDto;
import co.com.pragma.api.dto.request.RegisterApplicationRequestDto;
import co.com.pragma.model.application.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ApplicationMapperTest {

    private ApplicationMapper mapper;

    @BeforeEach
    void setup() {
        mapper = Mappers.getMapper(ApplicationMapper.class);
    }

    @Test
    @DisplayName("Should map RegisterApplicationRequestDto to User entity correctly")
    void testToEntity() {
        RegisterApplicationRequestDto dto = new RegisterApplicationRequestDto(
                20000.0,
                12,
                "fabricio@example.com",
                UUID.randomUUID()
        );

        Application application = mapper.toEntity(dto);

        assertNotNull(application);
        assertEquals(dto.amount(), application.getAmount());
        assertEquals(dto.term(), application.getTerm());
        assertEquals(dto.idDocument(), application.getIdDocument());
        assertEquals(dto.idLoanType(), application.getIdLoanType());

        assertNull(application.getId());
        assertNull(application.getIdStatus());
        assertNull(application.getEmail());
    }

    @Test
    @DisplayName("Should map Application entity to ApplicationDto correctly")
    void testToResponse() {
        Application application = Application.builder()
                .id(UUID.randomUUID())
                .amount(20000.0)
                .term(12)
                .email("fabricio@example.com")
                .idDocument("77777777")
                .idStatus(UUID.randomUUID())
                .idLoanType(UUID.randomUUID())
                .build();

        ApplicationDto dto = mapper.toResponse(application);

        assertNotNull(dto);
        assertEquals(application.getId(), dto.id());
        assertEquals(application.getAmount(), dto.amount());
        assertEquals(application.getTerm(), dto.term());
        assertEquals(application.getEmail(), dto.email());
        assertEquals(application.getIdDocument(), dto.idDocument());
        assertEquals(application.getIdStatus(), dto.idStatus());
        assertEquals(application.getIdLoanType(), dto.idLoanType());
    }

    @Test
    @DisplayName("Should return null when mapping null values")
    void testNullHandling() {
        assertNull(mapper.toEntity(null));
        assertNull(mapper.toResponse(null));
    }
}
