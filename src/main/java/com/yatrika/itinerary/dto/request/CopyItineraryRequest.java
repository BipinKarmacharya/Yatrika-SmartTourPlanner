package com.yatrika.itinerary.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CopyItineraryRequest {
    private LocalDate startDate; // Optional: If provided, we calculate the new end date
}