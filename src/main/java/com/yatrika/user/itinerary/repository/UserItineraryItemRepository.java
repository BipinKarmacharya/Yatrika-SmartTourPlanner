package com.yatrika.user.itinerary.repository;

import com.yatrika.user.itinerary.domain.UserItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserItineraryItemRepository extends JpaRepository<UserItineraryItem, Long> {

}
