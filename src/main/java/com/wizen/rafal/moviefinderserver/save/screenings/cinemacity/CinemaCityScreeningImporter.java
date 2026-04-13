package com.wizen.rafal.moviefinderserver.save.screenings.cinemacity;

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
import com.wizen.rafal.moviefinderserver.save.screenings.cinemacity.config.CinemaCityScreeningConfig;
import com.wizen.rafal.moviefinderserver.save.screenings.cinemacity.dto.CinemaCityScreeningResponse;
import com.wizen.rafal.moviefinderserver.save.screenings.config.ScreeningFetchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CinemaCityScreeningImporter implements ScreeningImporter {

    private static final String PROVIDER_CODE = "CINEMA_CITY";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final MovieSourceRepository movieSourceRepository;
    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final CinemaRepository cinemaRepository;
    private final ProviderService providerService;
    private final CinemaCityScreeningConfig config;
    private final ScreeningFetchProperties properties;
    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public void importScreenings() {
        if (!config.isEnabled()) {
            log.info("Cinema City screening import is disabled");
            return;
        }

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, "Cinema City");

        List<LocalDate> datesToFetch = prepareDatesList();

        log.info("Starting Cinema City screening fetch for {} days", datesToFetch.size());

        List<MovieSource> movieSources = movieSourceRepository.findByProviderId(provider.getId());
        log.info("Found {} movies for Cinema City provider", movieSources.size());

        int totalProcessed = 0;
        int totalScreeningsSaved = 0;
        int totalScreeningsSkipped = 0;

        for (MovieSource movieSource : movieSources) {
            try {
                ScreeningStats stats = processMovieSourceForAllDates(movieSource, datesToFetch, provider);
                totalScreeningsSaved += stats.saved;
                totalScreeningsSkipped += stats.skipped;
                totalProcessed++;

                log.info("Processed movie {}/{}: {} ({}), saved {} screenings, skipped {} duplicates",
                        totalProcessed, movieSources.size(),
                        movieSource.getExternalMovieId(), movieSource.getMovieId(),
                        stats.saved, stats.skipped);

                Thread.sleep(500);

            } catch (Exception e) {
                log.error("Error processing movie source {}: {}",
                        movieSource.getExternalMovieId(), e.getMessage(), e);
            }
        }

        log.info("Cinema City screening fetch completed. Processed {} movies, saved {} new screenings, skipped {} duplicates",
                totalProcessed, totalScreeningsSaved, totalScreeningsSkipped);
    }

    private List<LocalDate> prepareDatesList() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int daysAhead = properties.getDaysAhead();

        for (int i = 0; i <= daysAhead; i++) {
            dates.add(today.plusDays(i));
        }

        return dates;
    }

    private ScreeningStats processMovieSourceForAllDates(MovieSource movieSource, List<LocalDate> dates, CinemaProvider provider) {
        ScreeningStats totalStats = new ScreeningStats();

        for (LocalDate date : dates) {
            try {
                ScreeningStats stats = processMovieSourceForDate(movieSource, date, provider);
                totalStats.saved += stats.saved;
                totalStats.skipped += stats.skipped;

                Thread.sleep(200);
            } catch (Exception e) {
                log.error("Error processing movie {} for date {}: {}",
                        movieSource.getExternalMovieId(), date, e.getMessage());
            }
        }

        return totalStats;
    }

    private ScreeningStats processMovieSourceForDate(MovieSource movieSource, LocalDate date, CinemaProvider provider) {
        String formattedDate = date.format(DateTimeFormatter.ISO_DATE);
        String url = String.format(config.getUrlTemplate(), movieSource.getExternalMovieId(), formattedDate);

        log.debug("Fetching screenings for movie {} on date {}", movieSource.getExternalMovieId(), formattedDate);

        CinemaCityScreeningResponse response;
        try {
            response = restTemplate.getForObject(url, CinemaCityScreeningResponse.class);
        } catch (Exception e) {
            log.error("Error fetching screenings for movie {} on date {}: {}",
                    movieSource.getExternalMovieId(), formattedDate, e.getMessage());
            return new ScreeningStats();
        }

        ScreeningStats stats = new ScreeningStats();

        if (response == null || response.getBody() == null || response.getBody().getEvents() == null) {
            log.debug("No screenings found for movie {} on date {}",
                    movieSource.getExternalMovieId(), date);
            return stats;
        }

        List<CinemaCityScreeningResponse.Event> events = response.getBody().getEvents();

        for (CinemaCityScreeningResponse.Event event : events) {
            try {
                boolean saved = saveScreeningIfNotExists(movieSource, event, provider);
                if (saved) {
                    stats.saved++;
                } else {
                    stats.skipped++;
                }
            } catch (Exception e) {
                log.error("Error saving screening for movie {}, cinema {}: {}",
                        movieSource.getExternalMovieId(), event.getCinemaId(), e.getMessage());
            }
        }

        return stats;
    }

    private boolean saveScreeningIfNotExists(MovieSource movieSource, CinemaCityScreeningResponse.Event event, CinemaProvider provider) {
        Long cinemaId = parseCinemaId(event.getCinemaId());
        LocalDateTime screeningDateTime = parseDateTime(event.getEventDateTime());

        boolean exists = screeningRepository.existsByMovieIdAndCinemaIdAndScreeningDatetime(
                movieSource.getMovieId(),
                cinemaId,
                screeningDateTime
        );

        if (exists) {
            log.debug("Screening already exists for movie {}, cinema {}, datetime {}",
                    movieSource.getMovieId(), cinemaId, screeningDateTime);
            return false;
        }

        Cinema cinema = cinemaRepository.findByProviderIdAndExternalCinemaId(provider.getId(), cinemaId.toString())
                .orElseThrow(() -> new RuntimeException("Cinema not found: " + cinemaId));

        Movie movie = movieRepository.findById(movieSource.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieSource.getMovieId()));

        Screening screening = new Screening();
        screening.setMovie(movie);
        screening.setCinema(cinema);
        screening.setScreeningDatetime(screeningDateTime);
        screening.setScreeningUrl(event.getBookingLink());
        screening.setCreatedAt(LocalDateTime.now());

        screeningRepository.save(screening);
        return true;
    }

    private Long parseCinemaId(String cinemaIdStr) {
        try {
            return Long.parseLong(cinemaIdStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid cinema ID: " + cinemaIdStr);
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            log.error("Error parsing date time: {}", dateTimeStr);
            throw new RuntimeException("Invalid date time format: " + dateTimeStr);
        }
    }

    private static class ScreeningStats {
        int saved = 0;
        int skipped = 0;
    }
}
