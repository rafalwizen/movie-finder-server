package com.wizen.rafal.moviefinderserver.save;

import com.wizen.rafal.moviefinderserver.save.cinemas.CinemaImporter;
import com.wizen.rafal.moviefinderserver.save.movies.FilmImporter;
import com.wizen.rafal.moviefinderserver.save.screenings.ScreeningImporter;
import com.wizen.rafal.moviefinderserver.save.screenings.service.ScreeningCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final List<CinemaImporter> cinemaImporters;
    private final List<FilmImporter> filmImporters;
    private final List<ScreeningImporter> screeningImporters;
    private final ScreeningCleanupService screeningCleanupService;

    public void importCinemas() {
        importCinemas(null);
    }

    public void importCinemas(String providerCode) {
        List<CinemaImporter> filtered = filterByProvider(cinemaImporters,
                CinemaImporter::getProviderCode, providerCode);

        if (filtered.isEmpty()) {
            log.warn("No cinema importers found for provider: {}", providerCode);
            return;
        }

        log.info("Starting cinema fetch for {} provider(s)", filtered.size());

        for (CinemaImporter importer : filtered) {
            try {
                importer.importCinemas();
            } catch (Exception e) {
                log.error("Cinema fetch failed for provider {}", importer.getProviderCode(), e);
            }
        }

        log.info("Cinema fetch process completed");
    }

    public void importFilms() {
        importFilms(null);
    }

    public void importFilms(String providerCode) {
        List<FilmImporter> filtered = filterByProvider(filmImporters,
                FilmImporter::getProviderCode, providerCode);

        if (filtered.isEmpty()) {
            log.warn("No film importers found for provider: {}", providerCode);
            return;
        }

        log.info("Starting film download for {} provider(s)", filtered.size());

        for (FilmImporter importer : filtered) {
            try {
                importer.importFilms();
            } catch (Exception e) {
                log.error("Film download failed for provider {}", importer.getProviderCode(), e);
            }
        }

        log.info("Film download completed");
    }

    public void importScreenings() {
        importScreenings(null);
    }

    public void importScreenings(String providerCode) {
        List<ScreeningImporter> filtered = filterByProvider(screeningImporters,
                ScreeningImporter::getProviderCode, providerCode);

        if (filtered.isEmpty()) {
            log.warn("No screening importers found for provider: {}", providerCode);
            return;
        }

        log.info("Starting screening fetch for {} provider(s)", filtered.size());

        try {
            screeningCleanupService.cleanupOldScreenings();

            for (ScreeningImporter importer : filtered) {
                try {
                    importer.importScreenings();
                } catch (Exception e) {
                    log.error("Screening fetch failed for provider {}", importer.getProviderCode(), e);
                }
            }

            log.info("Screening fetch process completed");
        } catch (Exception e) {
            log.error("Screening fetch process failed", e);
        }
    }

    public void importAll() {
        log.info("Starting full import (cinemas -> films -> screenings)");
        importCinemas();
        importFilms();
        importScreenings();
        log.info("Full import completed");
    }

    private <T> List<T> filterByProvider(List<T> importers, java.util.function.Function<T, String> codeExtractor, String providerCode) {
        if (providerCode == null) {
            return importers;
        }
        return importers.stream()
                .filter(i -> codeExtractor.apply(i).equalsIgnoreCase(providerCode))
                .toList();
    }
}
