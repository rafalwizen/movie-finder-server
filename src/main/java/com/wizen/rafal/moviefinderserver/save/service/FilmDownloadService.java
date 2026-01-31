package com.wizen.rafal.moviefinderserver.save.service;

import com.wizen.rafal.moviefinderserver.save.config.CinemaCityProperties;
import com.wizen.rafal.moviefinderserver.save.dto.CinemaCityResponse;
import com.wizen.rafal.moviefinderserver.save.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.save.model.MovieSave;
import com.wizen.rafal.moviefinderserver.save.model.MovieSource;
import com.wizen.rafal.moviefinderserver.save.repository.CinemaProviderRepository;
import com.wizen.rafal.moviefinderserver.save.repository.MovieSaveRepository;
import com.wizen.rafal.moviefinderserver.save.repository.MovieSourceRepository;
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
	private final MovieSaveRepository movieRepository;
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
						if (movieSourceRepository.existsByProviderIdAndExternalMovieId(
								cinemaCityProvider.getId(), filmDto.getFilmId())) {
							log.debug("Film z external ID {} już istnieje dla providera {}, pomijam",
									filmDto.getFilmId(), CINEMA_CITY_PROVIDER_CODE);
							totalSkipped++;
							continue;
						}

						// Utwórz nowy film domenowy
						Long newMovieId = movieRepository.findMaxId() + 1;
						MovieSave movie = MovieSave.builder()
								.id(newMovieId)
								.title(filmDto.getFilmName())
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

						log.debug("Dodano film: {} (Movie ID: {}, External ID: {}, Provider: {})",
								movie.getTitle(), movie.getId(), filmDto.getFilmId(), CINEMA_CITY_PROVIDER_CODE);
						totalAdded++;
					}
				} else {
					log.warn("Brak danych w odpowiedzi dla kina {}", cinemaId);
				}

			} catch (Exception e) {
				log.error("Błąd podczas pobierania filmów dla kina {}: {}", cinemaId, e.getMessage(), e);
			}
		}

		log.info("Zakończono pobieranie filmów. Przetworzono: {}, Dodano: {}, Pominięto (duplikaty): {}",
				totalProcessed, totalAdded, totalSkipped);
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

	public List<MovieSave> getAllMovies() {
		return movieRepository.findAll();
	}
}
