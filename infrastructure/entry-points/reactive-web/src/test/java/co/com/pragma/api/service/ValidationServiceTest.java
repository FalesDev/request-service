package co.com.pragma.api.service;

import co.com.pragma.api.exception.ValidationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Map;

public class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setup() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            validationService = new ValidationService(validator);
        }
    }

    static class TestDto {
        @NotBlank(message = "name cannot be blank")
        String name;

        public TestDto(String name) {
            this.name = name;
        }
    }

    @Test
    @DisplayName("Should return object when valid")
    void testValidateSuccess() {
        TestDto dto = new TestDto("Fabricio");

        StepVerifier.create(validationService.validate(dto))
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return ValidationException when invalid")
    void testValidateFailure() {
        TestDto dto = new TestDto("");

        StepVerifier.create(validationService.validate(dto))
                .expectErrorMatches(throwable ->
                        throwable instanceof ValidationException &&
                                ((ValidationException) throwable).getErrors()
                                        .equals(Map.of("name", "name cannot be blank"))
                )
                .verify();
    }
}
