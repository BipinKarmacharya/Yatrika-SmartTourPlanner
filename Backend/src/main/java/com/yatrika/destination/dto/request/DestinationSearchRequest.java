package com.yatrika.destination.dto.request;

import com.yatrika.destination.domain.DestinationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@EqualsAndHashCode
public class DestinationSearchRequest {
    private String name;
    private String district;
    private String province;
    private DestinationType type;
    private String category;
    private Integer page = 0;
    private Integer size = 20;
    private String sortBy = "name";
    private Sort.Direction sortDirection = Sort.Direction.ASC;

    public Pageable toPageable() {
        return PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );
    }
}
