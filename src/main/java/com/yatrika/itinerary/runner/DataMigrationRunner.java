package com.yatrika.itinerary.runner;

import com.yatrika.itinerary.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataMigrationRunner implements CommandLineRunner {

    private final MigrationService migrationService;

    @Override
    public void run(String... args) {
        log.info("Starting itinerary data migration...");
        try {
            migrationService.migrateItineraryData();
            log.info("Itinerary data migration completed successfully");
        } catch (Exception e) {
            log.error("Error during itinerary data migration", e);
        }
    }
}