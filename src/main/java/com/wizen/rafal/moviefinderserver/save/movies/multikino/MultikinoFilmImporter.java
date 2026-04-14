package com.wizen.rafal.moviefinderserver.save.movies.multikino;

import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import com.wizen.rafal.moviefinderserver.domain.model.MovieSource;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieSourceRepository;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import com.wizen.rafal.moviefinderserver.save.movies.FilmImporter;
import com.wizen.rafal.moviefinderserver.save.movies.multikino.config.MultikinoFilmConfig;
import com.wizen.rafal.moviefinderserver.save.movies.multikino.dto.MultikinoFilmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultikinoFilmImporter implements FilmImporter {

    private static final String PROVIDER_CODE = "MULTIKINO";
    private static final String PROVIDER_NAME = "Multikino";

    private final MultikinoFilmConfig config;
    private final ProviderService providerService;
    private final MovieRepository movieRepository;
    private final MovieSourceRepository movieSourceRepository;
    private final RestTemplate restTemplate;

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    @Transactional
    public void importFilms() {
        if (!config.isEnabled()) {
            log.info("Multikino film import is disabled");
            return;
        }

        log.info("Starting film download from Multikino API");

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, PROVIDER_NAME);

        try {
            MultikinoFilmResponse response = restTemplate.getForObject(
                    config.getUrl(),
                    MultikinoFilmResponse.class
            );

            if (response == null || response.getResult() == null) {
                log.error("Received null response from Multikino films API");
                return;
            }

            List<MultikinoFilmResponse.FilmDto> films = response.getResult();
            log.info("Found {} films from Multikino", films.size());

            if (config.getLimit() > 0 && films.size() > config.getLimit()) {
                log.info("Limiting to {} films (config limit)", config.getLimit());
                films = films.subList(0, config.getLimit());
            }

            int totalAdded = 0;
            int totalSkipped = 0;

            for (MultikinoFilmResponse.FilmDto filmDto : films) {
                try {
                    Optional<MovieSource> existing = movieSourceRepository
                            .findByProviderIdAndExternalMovieId(provider.getId(), filmDto.getFilmId());

                    if (existing.isPresent()) {
                        totalSkipped++;
                        continue;
                    }

                    Long newMovieId = providerService.nextMovieId();
                    Movie movie = Movie.builder()
                            .id(newMovieId)
                            .title(filmDto.getFilmTitle())
                            .originalTitle(nullIfEmpty(filmDto.getOriginalTitle()))
                            .posterUrl(filmDto.getPosterImageSrc())
                            .durationMinutes(filmDto.getRunningTime())
                            .description(filmDto.getSynopsisShort())
                            .year(extractYear(filmDto.getReleaseDate()))
                            .build();
                    movieRepository.save(movie);

                    Long newSourceId = providerService.nextMovieSourceId();
                    MovieSource movieSource = MovieSource.builder()
                            .id(newSourceId)
                            .movieId(movie.getId())
                            .providerId(provider.getId())
                            .externalMovieId(filmDto.getFilmId())
                            .build();
                    movieSourceRepository.save(movieSource);

                    log.debug("Added Multikino film: {} (Movie ID: {}, External ID: {})",
                            movie.getTitle(), movie.getId(), filmDto.getFilmId());
                    totalAdded++;

                } catch (Exception e) {
                    log.error("Error processing Multikino film {}: {}", filmDto.getFilmTitle(), e.getMessage());
                }
            }

            log.info("Multikino film download completed. Added: {}, Skipped: {}", totalAdded, totalSkipped);

        } catch (Exception e) {
            log.error("Failed to fetch films from Multikino API", e);
            throw new RuntimeException("Multikino film download failed", e);
        }
    }

    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(releaseDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return date.getYear();
        } catch (Exception e) {
            try {
                return LocalDate.parse(releaseDate, DateTimeFormatter.ISO_LOCAL_DATE).getYear();
            } catch (Exception ex) {
                log.debug("Could not parse release date: {}", releaseDate);
                return null;
            }
        }
    }

    private String nullIfEmpty(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
