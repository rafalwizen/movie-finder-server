package com.wizen.rafal.moviefinderserver.save.movies.cinemacity;

import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import com.wizen.rafal.moviefinderserver.domain.model.MovieSource;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieSourceRepository;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import com.wizen.rafal.moviefinderserver.save.movies.FilmImporter;
import com.wizen.rafal.moviefinderserver.save.movies.cinemacity.config.CinemaCityFilmConfig;
import com.wizen.rafal.moviefinderserver.save.movies.cinemacity.dto.CinemaCityFilmDetailsResponse;
import com.wizen.rafal.moviefinderserver.save.movies.cinemacity.dto.CinemaCityFilmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CinemaCityFilmImporter implements FilmImporter {

    private static final String PROVIDER_CODE = "CINEMA_CITY";

    private final CinemaCityFilmConfig config;
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
            log.info("Cinema City film import is disabled");
            return;
        }

        log.info("Starting film download from Cinema City API");

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, "Cinema City");

        int totalProcessed = 0;
        int totalAdded = 0;
        int totalSkipped = 0;
        int totalUpdated = 0;

        for (String cinemaId : config.getCinemaIds()) {
            log.info("Fetching films for cinema ID: {}", cinemaId);

            try {
                String url = config.getBaseUrl() + "/" + cinemaId;
                CinemaCityFilmResponse response = restTemplate.getForObject(url, CinemaCityFilmResponse.class);

                if (response != null && response.getBody() != null) {
                    List<CinemaCityFilmResponse.FilmDto> films = response.getBody();
                    log.info("Found {} films for cinema {}", films.size(), cinemaId);

                    for (CinemaCityFilmResponse.FilmDto filmDto : films) {
                        totalProcessed++;

                        Optional<MovieSource> existingMovieSource = movieSourceRepository
                                .findByProviderIdAndExternalMovieId(provider.getId(), filmDto.getFilmId());

                        if (existingMovieSource.isPresent()) {
                            Long existingMovieId = existingMovieSource.get().getMovieId();
                            Optional<Movie> existingMovie = movieRepository.findById(existingMovieId);

                            if (existingMovie.isPresent()) {
                                Movie movie = existingMovie.get();

                                if (needsDetailsBackfill(movie)) {
                                    log.debug("Film {} (ID: {}) has missing details, fetching...",
                                            movie.getTitle(), movie.getId());

                                    CinemaCityFilmDetailsResponse.FilmDetails details =
                                            fetchFilmDetails(filmDto.getFilmId());
                                    if (applyDetails(movie, details)) {
                                        movieRepository.save(movie);
                                        log.info("Updated details for film: {} (ID: {})",
                                                movie.getTitle(), movie.getId());
                                        totalUpdated++;
                                    }
                                }
                            }

                            totalSkipped++;
                            continue;
                        }

                        CinemaCityFilmDetailsResponse.FilmDetails details = fetchFilmDetails(filmDto.getFilmId());

                        Movie movie = providerService.resolveMovie(
                                filmDto.getFilmName(),
                                details != null ? details.getOriginalName() : null,
                                details != null ? pickPosterUrl(details) : null,
                                details != null ? details.getLength() : null,
                                details != null ? details.getSynopsis() : null,
                                details != null ? parseYear(details.getReleaseYear()) : null);

                        Long newSourceId = providerService.nextMovieSourceId();
                        MovieSource movieSource = MovieSource.builder()
                                .id(newSourceId)
                                .movieId(movie.getId())
                                .providerId(provider.getId())
                                .externalMovieId(filmDto.getFilmId())
                                .build();
                        movieSourceRepository.save(movieSource);

                        log.debug("Added film: {} (Movie ID: {}, External ID: {})",
                                movie.getTitle(), movie.getId(), filmDto.getFilmId());
                        totalAdded++;
                    }
                } else {
                    log.warn("No data in response for cinema {}", cinemaId);
                }

            } catch (Exception e) {
                log.error("Error fetching films for cinema {}: {}", cinemaId, e.getMessage(), e);
            }
        }

        log.info("Cinema City film download completed. Processed: {}, Added: {}, Skipped: {}, Updated details: {}",
                totalProcessed, totalAdded, totalSkipped, totalUpdated);
    }

    /**
     * Whether the existing movie is missing any field the Cinema City details endpoint can provide.
     */
    private boolean needsDetailsBackfill(Movie movie) {
        return movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty()
                || movie.getDescription() == null || movie.getDescription().isEmpty()
                || movie.getDurationMinutes() == null
                || movie.getYear() == null;
    }

    /**
     * Fills only the blank fields of the movie from the fetched details.
     *
     * @return true when at least one field was set (movie should be saved)
     */
    private boolean applyDetails(Movie movie, CinemaCityFilmDetailsResponse.FilmDetails details) {
        if (details == null) {
            return false;
        }

        boolean changed = false;

        if ((movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty())
                && pickPosterUrl(details) != null) {
            movie.setPosterUrl(pickPosterUrl(details));
            changed = true;
        }
        if ((movie.getDescription() == null || movie.getDescription().isEmpty())
                && details.getSynopsis() != null && !details.getSynopsis().isEmpty()) {
            movie.setDescription(details.getSynopsis());
            changed = true;
        }
        if (movie.getDurationMinutes() == null && details.getLength() != null) {
            movie.setDurationMinutes(details.getLength());
            changed = true;
        }
        if (movie.getYear() == null && parseYear(details.getReleaseYear()) != null) {
            movie.setYear(parseYear(details.getReleaseYear()));
            changed = true;
        }

        return changed;
    }

    private String pickPosterUrl(CinemaCityFilmDetailsResponse.FilmDetails details) {
        if (details.getPosterLinkLarge() != null && !details.getPosterLinkLarge().isEmpty()) {
            return details.getPosterLinkLarge();
        }
        return details.getPosterLink();
    }

    private Integer parseYear(String releaseYear) {
        if (releaseYear == null || releaseYear.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(releaseYear.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse release year: {}", releaseYear);
            return null;
        }
    }

    private CinemaCityFilmDetailsResponse.FilmDetails fetchFilmDetails(String externalMovieId) {
        try {
            String url = config.getFilmDetailsUrl() + "/" + externalMovieId;
            log.debug("Fetching film details from URL: {}", url);

            CinemaCityFilmDetailsResponse response = restTemplate.getForObject(url, CinemaCityFilmDetailsResponse.class);

            if (response != null && response.getBody() != null
                    && response.getBody().getFilmDetails() != null) {
                return response.getBody().getFilmDetails();
            }
        } catch (Exception e) {
            log.error("Error fetching film details for film {}: {}", externalMovieId, e.getMessage());
        }

        return null;
    }
}
