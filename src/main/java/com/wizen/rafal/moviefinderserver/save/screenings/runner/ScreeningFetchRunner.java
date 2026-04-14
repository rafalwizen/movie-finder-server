package com.wizen.rafal.moviefinderserver.save.screenings.runner;

import com.wizen.rafal.moviefinderserver.save.screenings.ScreeningImporter;
import com.wizen.rafal.moviefinderserver.save.screenings.service.ScreeningCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("fetch-screenings")
@RequiredArgsConstructor
@Slf4j
public class ScreeningFetchRunner implements CommandLineRunner {

    private static final String PROVIDER_PROFILE_PREFIX = "fetch-screenings-";

    private final List<ScreeningImporter> importers;
    private final ScreeningCleanupService screeningCleanupService;
    private final Environment environment;

    @Override
    public void run(String... args) {
        String specificProvider = resolveSpecificProvider();

        List<ScreeningImporter> filteredImporters = specificProvider != null
                ? importers.stream().filter(i -> i.getProviderCode().equalsIgnoreCase(specificProvider)).toList()
                : importers;

        if (filteredImporters.isEmpty()) {
            log.warn("No screening importers found for provider: {}", specificProvider);
            return;
        }

        log.info("Starting screening fetch for {} provider(s)", filteredImporters.size());

        try {
            screeningCleanupService.cleanupOldScreenings();

            for (ScreeningImporter importer : filteredImporters) {
                try {
                    importer.importScreenings();
                } catch (Exception e) {
                    log.error("Screening fetch failed for provider {}", importer.getProviderCode(), e);
                }
            }

            log.info("Screening fetch process completed");
        } catch (Exception e) {
            log.error("Screening fetch process failed", e);
            System.exit(1);
        }
    }

    private String resolveSpecificProvider() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.startsWith(PROVIDER_PROFILE_PREFIX)) {
                return profile.substring(PROVIDER_PROFILE_PREFIX.length());
            }
        }
        return null;
    }
}
