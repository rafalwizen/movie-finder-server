package com.wizen.rafal.moviefinderserver.save.screenings.runner;

import com.wizen.rafal.moviefinderserver.save.screenings.ScreeningImporter;
import com.wizen.rafal.moviefinderserver.save.screenings.service.ScreeningCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("fetch-screenings")
@RequiredArgsConstructor
@Slf4j
public class ScreeningFetchRunner implements CommandLineRunner {

    private final List<ScreeningImporter> importers;
    private final ScreeningCleanupService screeningCleanupService;

    @Override
    public void run(String... args) {
        log.info("Starting screening fetch for {} providers", importers.size());

        try {
            screeningCleanupService.cleanupOldScreenings();

            for (ScreeningImporter importer : importers) {
                try {
                    importer.importScreenings();
                } catch (Exception e) {
                    log.error("Screening fetch failed for provider", e);
                }
            }

            log.info("Screening fetch process completed");
        } catch (Exception e) {
            log.error("Screening fetch process failed", e);
            System.exit(1);
        }
    }
}
