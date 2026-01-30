package com.wizen.rafal.moviefinderserver.save.service;

import com.wizen.rafal.moviefinderserver.save.config.CinemaCityProperties;
import com.wizen.rafal.moviefinderserver.save.dto.CinemaCityResponse;
import com.wizen.rafal.moviefinderserver.save.model.MovieSave;
import com.wizen.rafal.moviefinderserver.save.repository.MovieSaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmDownloadService {

	private final CinemaCityProperties cinemaCityProperties;
	private final MovieSaveRepository movieSaveRepository;
	private final RestTemplate restTemplate;


	@Transactional
	public void downloadAndSaveFilms() {
		log.info("Rozpoczynam pobieranie filmów z Cinema City API");

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

						if (movieSaveRepository.existsByOriginalId(filmDto.getFilmId())) {
							log.debug("Film {} już istnieje w bazie, pomijam", filmDto.getFilmId());
							totalSkipped++;
							continue;
						}

						Long newId = movieSaveRepository.findMaxId() + 1;

						MovieSave movie = MovieSave.builder()
								.id(newId)
								.originalId(filmDto.getFilmId())
								.title(filmDto.getFilmName())
								.build();

						movieSaveRepository.save(movie);
						log.debug("Dodano film: {} (ID: {}, originalId: {})", movie.getTitle(), movie.getId(), movie.getOriginalId());
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

	public List<MovieSave> getAllMovies() {
		return movieSaveRepository.findAll();
	}
}
