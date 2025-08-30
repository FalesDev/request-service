package co.com.pragma.api.service;

import co.com.pragma.api.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ValidationService {

    private final Validator validator;

    public <T> Mono<T> validate(T obj) {
        return Mono.fromCallable(() -> validator.validate(obj))
                .flatMap(violations -> violations.isEmpty()
                                ? Mono.just(obj)
                                : Mono.error(new ValidationException(
                                violations.stream()
                                        .collect(Collectors.groupingBy(
                                                v -> v.getPropertyPath().toString(),
                                                Collectors.mapping(
                                                        ConstraintViolation::getMessage,
                                                        Collectors.toList()
                                                )
                                        ))
                        ))
                );
    }
}
