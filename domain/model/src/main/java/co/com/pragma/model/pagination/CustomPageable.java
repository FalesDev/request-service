package co.com.pragma.model.pagination;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomPageable {
    private int page;
    private int size;
    private String sortBy;
    private String sortDirection;
}
