package com.wizen.rafal.moviefinderserver.save.cinemas.runner;

import com.wizen.rafal.moviefinderserver.save.cinemas.CinemaImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("fetch-cinemas")
@RequiredArgsConstructor
@Slf4j
public class CinemaFetchRunner implements CommandLineRunner {

    private final List<CinemaImporter> importers;

    @Override
    public void run(String... args) {
        log.info("Starting cinema fetch for {} providers", importers.size());

        for (CinemaImporter importer : importers) {
            try {
                importer.importCinemas();
            } catch (Exception e) {
                log.error("Cinema fetch failed for provider", e);
            }
        }

        log.info("Cinema fetch process completed");
    }
}
