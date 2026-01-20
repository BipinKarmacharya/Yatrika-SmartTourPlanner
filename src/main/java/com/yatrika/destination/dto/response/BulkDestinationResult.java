package com.yatrika.destination.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkDestinationResult {

    private int total;
    private int success;
    private int failed;

    private List<SuccessItem> successes;
    private List<FailureItem> failures;

    @Data
    @Builder
    public static class SuccessItem {
        private int index;
        private Long destinationId;
        private String name;
    }

    @Data
    @Builder
    public static class FailureItem {
        private int index;
        private String name;
        private String error;
    }
}
