package com.wizen.rafal.moviefinderserver.save.cinemas.runner;

import com.wizen.rafal.moviefinderserver.save.cinemas.CinemaImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("fetch-cinemas")
@RequiredArgsConstructor
@Slf4j
public class CinemaFetchRunner implements CommandLineRunner {

    private static final String PROVIDER_PROFILE_PREFIX = "fetch-cinemas-";

    private final List<CinemaImporter> importers;
    private final Environment environment;

    @Override
    public void run(String... args) {
        String specificProvider = resolveSpecificProvider();

        List<CinemaImporter> filteredImporters = specificProvider != null
                ? importers.stream().filter(i -> i.getProviderCode().equalsIgnoreCase(specificProvider)).toList()
                : importers;

        if (filteredImporters.isEmpty()) {
            log.warn("No cinema importers found for provider: {}", specificProvider);
            return;
        }

        log.info("Starting cinema fetch for {} provider(s)", filteredImporters.size());

        for (CinemaImporter importer : filteredImporters) {
            try {
                importer.importCinemas();
            } catch (Exception e) {
                log.error("Cinema fetch failed for provider {}", importer.getProviderCode(), e);
            }
        }

        log.info("Cinema fetch process completed");
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
