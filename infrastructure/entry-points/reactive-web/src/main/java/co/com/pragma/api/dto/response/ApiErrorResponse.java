package co.com.pragma.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, List<String>> errors
) {
}
