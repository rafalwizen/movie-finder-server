package com.wizen.rafal.moviefinderserver.save.movies.runner;

import com.wizen.rafal.moviefinderserver.save.movies.FilmImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("download-films")
@RequiredArgsConstructor
public class FilmDownloadRunner implements CommandLineRunner {

    private static final String PROVIDER_PROFILE_PREFIX = "download-films-";

    private final List<FilmImporter> importers;
    private final Environment environment;

    @Override
    public void run(String... args) {
        String specificProvider = resolveSpecificProvider();

        List<FilmImporter> filteredImporters = specificProvider != null
                ? importers.stream().filter(i -> i.getProviderCode().equalsIgnoreCase(specificProvider)).toList()
                : importers;

        if (filteredImporters.isEmpty()) {
            log.warn("No film importers found for provider: {}", specificProvider);
            return;
        }

        log.info("Starting film download for {} provider(s)", filteredImporters.size());

        for (FilmImporter importer : filteredImporters) {
            try {
                importer.importFilms();
            } catch (Exception e) {
                log.error("Film download failed for provider {}", importer.getProviderCode(), e);
            }
        }

        log.info("Film download completed");
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
