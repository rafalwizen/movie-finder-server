package com.wizen.rafal.moviefinderserver.save.screenings.multikino;

import com.wizen.rafal.moviefinderserver.domain.model.Cinema;
import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import com.wizen.rafal.moviefinderserver.domain.model.MovieSource;
import com.wizen.rafal.moviefinderserver.domain.model.Screening;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieSourceRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.ScreeningRepository;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import com.wizen.rafal.moviefinderserver.save.screenings.ScreeningImporter;
import com.wizen.rafal.moviefinderserver.save.screenings.multikino.config.MultikinoScreeningConfig;
import com.wizen.rafal.moviefinderserver.save.screenings.multikino.dto.MultikinoScreeningResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultikinoScreeningImporter implements ScreeningImporter {

    private static final String PROVIDER_CODE = "MULTIKINO";
    private static final String PROVIDER_NAME = "Multikino";
    private static final Pattern FILM_ID_PATTERN = Pattern.compile("/podsumowanie/\\d+/(.+?)/");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String MULTIKINO_BASE_URL = "https://www.multikino.pl";

    private final MovieSourceRepository movieSourceRepository;
    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final CinemaRepository cinemaRepository;
    private final ProviderService providerService;
    private final MultikinoScreeningConfig config;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public void importScreenings() {
        if (!config.isEnabled()) {
            log.info("Multikino screening import is disabled");
            return;
        }

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, PROVIDER_NAME);

        List<Cinema> cinemas = cinemaRepository.findAll().stream()
                .filter(c -> c.getProvider().getId().equals(provider.getId()))
                .toList();

        log.info("Starting Multikino screening fetch for {} cinemas", cinemas.size());

        int totalSaved = 0;
        int totalSkipped = 0;

        for (Cinema cinema : cinemas) {
            try {
                String url = config.getBaseUrl() + "/cinemas/" + cinema.getExternalCinemaId() + "/films";
                log.debug("Fetching screenings for Multikino cinema: {} ({})", cinema.getName(), cinema.getExternalCinemaId());

                MultikinoScreeningResponse response = restTemplate.getForObject(url, MultikinoScreeningResponse.class);

                if (response == null || response.getResult() == null) {
                    log.debug("No screenings response for cinema {}", cinema.getName());
                    rateLimitDelay();
                    continue;
                }

                for (MultikinoScreeningResponse.ShowingResult showingResult : response.getResult()) {
                    if (showingResult.getShowingGroups() == null) continue;

                    for (MultikinoScreeningResponse.ShowingGroup group : showingResult.getShowingGroups()) {
                        if (group.getSessions() == null) continue;

                        String filmId = extractFilmId(group.getSessions());
                        if (filmId == null) {
                            log.warn("Could not extract filmId from sessions for cinema {}", cinema.getName());
                            continue;
                        }

                        Optional<MovieSource> movieSourceOpt = movieSourceRepository
                                .findByProviderIdAndExternalMovieId(provider.getId(), filmId);

                        if (movieSourceOpt.isEmpty()) {
                            log.debug("MovieSource not found for Multikino filmId {}, skipping screenings", filmId);
                            continue;
                        }

                        MovieSource movieSource = movieSourceOpt.get();

                        for (MultikinoScreeningResponse.Session session : group.getSessions()) {
                            try {
                                LocalDateTime screeningTime = parseDateTime(session.getStartTime());

                                boolean exists = screeningRepository.existsByMovieIdAndCinemaIdAndScreeningDatetime(
                                        movieSource.getMovieId(), cinema.getId(), screeningTime);

                                if (exists) {
                                    totalSkipped++;
                                    continue;
                                }

                                Movie movie = movieRepository.findById(movieSource.getMovieId())
                                        .orElseThrow(() -> new RuntimeException("Movie not found: " + movieSource.getMovieId()));

                                Screening screening = new Screening();
                                screening.setMovie(movie);
                                screening.setCinema(cinema);
                                screening.setScreeningDatetime(screeningTime);
                                screening.setScreeningUrl(MULTIKINO_BASE_URL + session.getBookingUrl());
                                screening.setCreatedAt(LocalDateTime.now());

                                screeningRepository.save(screening);
                                totalSaved++;

                            } catch (Exception e) {
                                log.error("Error saving Multikino screening for session {}: {}",
                                        session.getSessionId(), e.getMessage());
                            }
                        }
                    }
                }

                log.info("Processed Multikino cinema: {} (saved screenings so far: {})", cinema.getName(), totalSaved);
                rateLimitDelay();

            } catch (Exception e) {
                log.error("Error fetching screenings for Multikino cinema {}: {}",
                        cinema.getName(), e.getMessage(), e);
            }
        }

        log.info("Multikino screening fetch completed. Saved: {}, Skipped: {}", totalSaved, totalSkipped);
    }

    private String extractFilmId(List<MultikinoScreeningResponse.Session> sessions) {
        for (MultikinoScreeningResponse.Session session : sessions) {
            if (session.getBookingUrl() != null) {
                Matcher matcher = FILM_ID_PATTERN.matcher(session.getBookingUrl());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            throw new RuntimeException("Invalid date time format: " + dateTimeStr, e);
        }
    }

    private void rateLimitDelay() {
        try {
            Thread.sleep(config.getRateLimitMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
