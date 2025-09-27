package co.com.pragma.usecase.findloantypebyid;

import co.com.pragma.model.exception.EntityNotFoundException;
import co.com.pragma.model.gateways.CustomLogger;
import co.com.pragma.model.loantype.LoanType;
import co.com.pragma.model.loantype.gateways.LoanTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindLoanTypeByIdUseCaseTest {

    @Mock
    private LoanTypeRepository loanTypeRepository;

    @Mock
    private CustomLogger customLogger;

    @InjectMocks
    private FindLoanTypeByIdUseCase findLoanTypeByIdUseCase;

    private UUID loanTypeId;
    private LoanType expectedLoanType;

    @BeforeEach
    void setUp() {
        loanTypeId = UUID.randomUUID();
        expectedLoanType = LoanType.builder()
                .id(loanTypeId)
                .name("Personal Loan")
                .minAmount(1000.0)
                .maxAmount(50000.0)
                .interestRate(12.5)
                .automaticValidation(true)
                .build();
    }

    @Test
    @DisplayName("Should find loan type by id successfully")
    void shouldFindLoanTypeByIdSuccessfully() {
        when(loanTypeRepository.findById(loanTypeId)).thenReturn(Mono.just(expectedLoanType));

        StepVerifier.create(findLoanTypeByIdUseCase.findById(loanTypeId))
                .expectNext(expectedLoanType)
                .verifyComplete();

        verify(customLogger).trace("Finding LoanType by id: {}", loanTypeId);
        verify(customLogger).trace("LoanType found successfully with id: {}", loanTypeId);
        verify(loanTypeRepository).findById(loanTypeId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when loan type not found")
    void shouldThrowEntityNotFoundExceptionWhenLoanTypeNotFound() {
        when(loanTypeRepository.findById(loanTypeId)).thenReturn(Mono.empty());

        StepVerifier.create(findLoanTypeByIdUseCase.findById(loanTypeId))
                .expectError(EntityNotFoundException.class)
                .verify();

        verify(customLogger).trace("Finding LoanType by id: {}", loanTypeId);
        verify(customLogger).trace(eq("Error searching LoanType by id: {}, error: {}"), eq(loanTypeId), any(String.class));
        verify(loanTypeRepository).findById(loanTypeId);
    }

    @Test
    @DisplayName("Should log error message when repository throws exception")
    void shouldLogErrorMessageWhenRepositoryThrowsException() {
        RuntimeException repositoryException = new RuntimeException("Database connection failed");
        when(loanTypeRepository.findById(loanTypeId)).thenReturn(Mono.error(repositoryException));

        StepVerifier.create(findLoanTypeByIdUseCase.findById(loanTypeId))
                .expectError(RuntimeException.class)
                .verify();

        verify(customLogger).trace("Finding LoanType by id: {}", loanTypeId);
        verify(customLogger).trace("Error searching LoanType by id: {}, error: {}", loanTypeId, "Database connection failed");
        verify(loanTypeRepository).findById(loanTypeId);
    }
}