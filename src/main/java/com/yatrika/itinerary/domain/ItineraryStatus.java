package com.yatrika.itinerary.domain;

public enum ItineraryStatus {
    TEMPLATE,   // Created by Admin, used as a "base" for others
    DRAFT,      // User is currently editing/planning
    PLANNED,    // User has finalized the plan but hasn't traveled yet
    COMPLETED,  // Trip is finished (only these can be shared to Public)
    ARCHIVED    // Deleted/hidden by user
}
