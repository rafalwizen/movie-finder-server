package com.wizen.rafal.moviefinderserver.save.movies.runner;

import com.wizen.rafal.moviefinderserver.save.movies.FilmImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("download-films")
@RequiredArgsConstructor
public class FilmDownloadRunner implements CommandLineRunner {

    private final List<FilmImporter> importers;

    @Override
    public void run(String... args) {
        log.info("Starting film download for {} providers", importers.size());

        for (FilmImporter importer : importers) {
            try {
                importer.importFilms();
            } catch (Exception e) {
                log.error("Film download failed for provider", e);
            }
        }

        log.info("Film download completed");
    }
}
