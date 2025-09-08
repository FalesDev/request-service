package co.com.pragma.model.pagination;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomPage<T> {
    private List<T> content;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
