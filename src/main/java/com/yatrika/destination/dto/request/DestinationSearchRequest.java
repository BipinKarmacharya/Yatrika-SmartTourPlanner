package com.yatrika.destination.dto.request;

import com.yatrika.destination.domain.DestinationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode
public class DestinationSearchRequest {
    private String name;      // Maps to Flutter's _searchQuery
    private String district;
    private String province;
    private DestinationType type;
    private String category;
    private List<String> tags; // Maps to Flutter's _appliedTags
    private BigDecimal minPrice; // Matches Flutter's budget logic
    private BigDecimal maxPrice;

    // Pagination fields
    private int page = 0;
    private int size = 20;
    private String sortBy = "name";
    private String sortDirection = "ASC";

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy);
    }
}