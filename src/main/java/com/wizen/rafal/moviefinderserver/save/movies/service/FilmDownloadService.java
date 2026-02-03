package com.wizen.rafal.moviefinderserver.save.movies.service;

import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieRepository;
import com.wizen.rafal.moviefinderserver.save.movies.config.CinemaCityProperties;
import com.wizen.rafal.moviefinderserver.save.movies.dto.CinemaCityResponse;
import com.wizen.rafal.moviefinderserver.save.movies.dto.FilmDetailsResponse;
import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.model.MovieSource;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaProviderRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmDownloadService {

	private static final String CINEMA_CITY_PROVIDER_CODE = "CINEMA_CITY";

	private final CinemaCityProperties cinemaCityProperties;
	private final MovieRepository movieRepository;
	private final MovieSourceRepository movieSourceRepository;
	private final CinemaProviderRepository cinemaProviderRepository;
	private final RestTemplate restTemplate;

	@Transactional
	public void downloadAndSaveFilms() {
		log.info("Rozpoczynam pobieranie filmów z Cinema City API");

		// Pobierz lub utwórz providera Cinema City
		CinemaProvider cinemaCityProvider = getOrCreateCinemaCityProvider();

		int totalProcessed = 0;
		int totalAdded = 0;
		int totalSkipped = 0;
		int totalUpdated = 0;

		for (String cinemaId : cinemaCityProperties.getCinemaIds()) {
			log.info("Pobieram filmy dla kina o ID: {}", cinemaId);

			try {
				String url = cinemaCityProperties.getBaseUrl() + "/" + cinemaId;
				CinemaCityResponse response = restTemplate.getForObject(url, CinemaCityResponse.class);

				if (response != null && response.getBody() != null) {
					List<CinemaCityResponse.FilmDto> films = response.getBody();
					log.info("Znaleziono {} filmów dla kina {}", films.size(), cinemaId);

					for (CinemaCityResponse.FilmDto filmDto : films) {
						totalProcessed++;

						// Sprawdź czy film z tym external_movie_id już istnieje dla tego providera
						Optional<MovieSource> existingMovieSource = movieSourceRepository
								.findByProviderIdAndExternalMovieId(cinemaCityProvider.getId(), filmDto.getFilmId());

						if (existingMovieSource.isPresent()) {
							// Film już istnieje - sprawdź czy ma plakat
							Long existingMovieId = existingMovieSource.get().getMovieId();
							Optional<Movie> existingMovie = movieRepository.findById(existingMovieId);

							if (existingMovie.isPresent()) {
								Movie movie = existingMovie.get();

								if (movie.getPosterUrl() == null || movie.getPosterUrl().isEmpty()) {
									// Film nie ma plakatu - pobierz i uzupełnij
									log.debug("Film {} (ID: {}) nie ma plakatu, uzupełniam...",
											movie.getTitle(), movie.getId());

									String posterUrl = fetchPosterUrl(filmDto.getFilmId());
									if (posterUrl != null) {
										movie.setPosterUrl(posterUrl);
										movieRepository.save(movie);
										log.info("Uzupełniono plakat dla filmu: {} (ID: {})",
												movie.getTitle(), movie.getId());
										totalUpdated++;
									} else {
										log.debug("Nie udało się pobrać plakatu dla filmu {}", movie.getTitle());
									}
								} else {
									log.debug("Film {} (ID: {}) już ma plakat, pomijam",
											movie.getTitle(), movie.getId());
								}
							}

							totalSkipped++;
							continue;
						}

						// Film nie istnieje - pobierz plakat i utwórz nowy film
						String posterUrl = fetchPosterUrl(filmDto.getFilmId());

						// Utwórz nowy film domenowy
						Long newMovieId = movieRepository.findMaxId() + 1;
						Movie movie = Movie.builder()
								.id(newMovieId)
								.title(filmDto.getFilmName())
								.posterUrl(posterUrl)
								.build();
						movieRepository.save(movie);

						// Utwórz powiązanie filmu z provideram (movie_source)
						Long newSourceId = movieSourceRepository.findMaxId() + 1;
						MovieSource movieSource = MovieSource.builder()
								.id(newSourceId)
								.movieId(movie.getId())
								.providerId(cinemaCityProvider.getId())
								.externalMovieId(filmDto.getFilmId())
								.build();
						movieSourceRepository.save(movieSource);

						log.debug("Dodano film: {} (Movie ID: {}, External ID: {}, Provider: {}, Poster: {})",
								movie.getTitle(), movie.getId(), filmDto.getFilmId(),
								CINEMA_CITY_PROVIDER_CODE, posterUrl != null ? "TAK" : "BRAK");
						totalAdded++;
					}
				} else {
					log.warn("Brak danych w odpowiedzi dla kina {}", cinemaId);
				}

			} catch (Exception e) {
				log.error("Błąd podczas pobierania filmów dla kina {}: {}", cinemaId, e.getMessage(), e);
			}
		}

		log.info("Zakończono pobieranie filmów. Przetworzono: {}, Dodano: {}, Pominięto: {}, Zaktualizowano plakatów: {}",
				totalProcessed, totalAdded, totalSkipped, totalUpdated);
	}

	private String fetchPosterUrl(String externalMovieId) {
		try {
			String url = cinemaCityProperties.getFilmDetailsUrl() + "/" + externalMovieId;
			log.debug("Pobieram szczegóły filmu z URL: {}", url);

			FilmDetailsResponse response = restTemplate.getForObject(url, FilmDetailsResponse.class);

			if (response != null && response.getBody() != null
					&& response.getBody().getFilmDetails() != null) {
				String posterLink = response.getBody().getFilmDetails().getPosterLink();

				if (posterLink != null && !posterLink.isEmpty()) {
					log.debug("Znaleziono plakat dla filmu {}: {}", externalMovieId, posterLink);
					return posterLink;
				} else {
					log.debug("Brak plakatu dla filmu {}", externalMovieId);
				}
			} else {
				log.warn("Nieprawidłowa odpowiedź przy pobieraniu szczegółów filmu {}", externalMovieId);
			}
		} catch (Exception e) {
			log.error("Błąd podczas pobierania plakatu dla filmu {}: {}", externalMovieId, e.getMessage());
		}

		return null;
	}

	private CinemaProvider getOrCreateCinemaCityProvider() {
		Optional<CinemaProvider> existingProvider = cinemaProviderRepository.findByCode(CINEMA_CITY_PROVIDER_CODE);

		if (existingProvider.isPresent()) {
			log.debug("Provider {} już istnieje w bazie", CINEMA_CITY_PROVIDER_CODE);
			return existingProvider.get();
		}

		// Jeśli provider nie istnieje, utwórz go
		Long newProviderId = cinemaProviderRepository.findAll().stream()
				.map(CinemaProvider::getId)
				.max(Long::compareTo)
				.orElse(0L) + 1;

		CinemaProvider newProvider = CinemaProvider.builder()
				.id(newProviderId)
				.code(CINEMA_CITY_PROVIDER_CODE)
				.name("Cinema City")
				.build();

		cinemaProviderRepository.save(newProvider);
		log.info("Utworzono nowego providera: {} (ID: {})", newProvider.getName(), newProvider.getId());

		return newProvider;
	}

	public List<Movie> getAllMovies() {
		return movieRepository.findAll();
	}
}
