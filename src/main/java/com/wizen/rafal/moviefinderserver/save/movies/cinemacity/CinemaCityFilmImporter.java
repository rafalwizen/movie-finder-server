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

                                if (movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty()) {
                                    log.debug("Film {} (ID: {}) has no poster, fetching...",
                                            movie.getTitle(), movie.getId());

                                    String posterUrl = fetchPosterUrl(filmDto.getFilmId());
                                    if (posterUrl != null) {
                                        movie.setPosterUrl(posterUrl);
                                        movieRepository.save(movie);
                                        log.info("Updated poster for film: {} (ID: {})",
                                                movie.getTitle(), movie.getId());
                                        totalUpdated++;
                                    }
                                }
                            }

                            totalSkipped++;
                            continue;
                        }

                        String posterUrl = fetchPosterUrl(filmDto.getFilmId());

                        Long newMovieId = providerService.nextMovieId();
                        Movie movie = Movie.builder()
                                .id(newMovieId)
                                .title(filmDto.getFilmName())
                                .posterUrl(posterUrl)
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

        log.info("Cinema City film download completed. Processed: {}, Added: {}, Skipped: {}, Updated posters: {}",
                totalProcessed, totalAdded, totalSkipped, totalUpdated);
    }

    private String fetchPosterUrl(String externalMovieId) {
        try {
            String url = config.getFilmDetailsUrl() + "/" + externalMovieId;
            log.debug("Fetching film details from URL: {}", url);

            CinemaCityFilmDetailsResponse response = restTemplate.getForObject(url, CinemaCityFilmDetailsResponse.class);

            if (response != null && response.getBody() != null
                    && response.getBody().getFilmDetails() != null) {
                String posterLink = response.getBody().getFilmDetails().getPosterLink();

                if (posterLink != null && !posterLink.isEmpty()) {
                    log.debug("Found poster for film {}: {}", externalMovieId, posterLink);
                    return posterLink;
                }
            }
        } catch (Exception e) {
            log.error("Error fetching poster for film {}: {}", externalMovieId, e.getMessage());
        }

        return null;
    }
}
