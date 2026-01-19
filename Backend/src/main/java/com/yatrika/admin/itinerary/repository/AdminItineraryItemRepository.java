package com.yatrika.admin.itinerary.repository;

import com.yatrika.admin.itinerary.domain.AdminItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminItineraryItemRepository
        extends JpaRepository<AdminItineraryItem, Long> {

    List<AdminItineraryItem> findByAdminItineraryIdOrderByDayNumberAscOrderInDayAsc(Long itineraryId);
}
