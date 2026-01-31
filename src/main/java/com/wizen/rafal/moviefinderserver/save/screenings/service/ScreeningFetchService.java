package com.wizen.rafal.moviefinderserver.save.screenings.service;

import com.wizen.rafal.moviefinderserver.save.screenings.dto.CinemaCityResponse;
import com.wizen.rafal.moviefinderserver.save.screenings.model.CinemaScreening;
import com.wizen.rafal.moviefinderserver.save.screenings.model.MovieSourceScreening;
import com.wizen.rafal.moviefinderserver.save.screenings.model.ScreeningScreening;
import com.wizen.rafal.moviefinderserver.save.screenings.repository.CinemaScreeningRepository;
import com.wizen.rafal.moviefinderserver.save.screenings.repository.MovieSourceScreeningRepository;
import com.wizen.rafal.moviefinderserver.save.screenings.repository.ScreeningScreeningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningFetchService {

	private final MovieSourceScreeningRepository movieSourceRepository;
	private final ScreeningScreeningRepository screeningRepository;
	private final CinemaScreeningRepository cinemaRepository;
	private final CinemaCityApiService cinemaCityApiService;

	@Value("${screening-fetch.date:#{T(java.time.LocalDate).now()}}")
	private LocalDate fetchDate;

	private static final Long CINEMA_CITY_PROVIDER_ID = 1L;
	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Transactional
	public void fetchAllScreenings() {
		log.info("Starting to fetch screenings for date: {}", fetchDate);

		List<MovieSourceScreening> movieSources = movieSourceRepository.findByProviderId(CINEMA_CITY_PROVIDER_ID);
		log.info("Found {} movies for Cinema City provider", movieSources.size());

		int totalProcessed = 0;
		int totalScreeningsSaved = 0;

		for (MovieSourceScreening movieSource : movieSources) {
			try {
				int screeningsSaved = processMovieSource(movieSource);
				totalScreeningsSaved += screeningsSaved;
				totalProcessed++;

				log.info("Processed movie {}/{}: {} ({}), saved {} screenings",
						totalProcessed, movieSources.size(),
						movieSource.getExternalMovieId(), movieSource.getMovieId(),
						screeningsSaved);

				// Delay to avoid API rate limiting
				Thread.sleep(500);

			} catch (Exception e) {
				log.error("Error processing movie source {}: {}",
						movieSource.getExternalMovieId(), e.getMessage(), e);
			}
		}

		log.info("Finished fetching screenings. Processed {} movies, saved {} screenings",
				totalProcessed, totalScreeningsSaved);
	}

	private int processMovieSource(MovieSourceScreening movieSource) {
		CinemaCityResponse response = cinemaCityApiService.fetchScreenings(
				movieSource.getExternalMovieId(),
				fetchDate
		);

		if (response == null || response.getBody() == null || response.getBody().getEvents() == null) {
			log.debug("No screenings found for movie {}", movieSource.getExternalMovieId());
			return 0;
		}

		List<CinemaCityResponse.Event> events = response.getBody().getEvents();
		int savedCount = 0;

		for (CinemaCityResponse.Event event : events) {
			try {
				saveScreening(movieSource, event);
				savedCount++;
			} catch (Exception e) {
				log.error("Error saving screening for movie {}, cinema {}: {}",
						movieSource.getExternalMovieId(), event.getCinemaId(), e.getMessage());
			}
		}

		return savedCount;
	}

	private void saveScreening(MovieSourceScreening movieSource, CinemaCityResponse.Event event) {
		Long cinemaId = parseCinemaId(event.getCinemaId());

		// Verify cinema exists
		CinemaScreening cinema = cinemaRepository.findByIdAndProviderId(cinemaId, CINEMA_CITY_PROVIDER_ID)
				.orElseThrow(() -> new RuntimeException("Cinema not found: " + cinemaId));

		ScreeningScreening screening = new ScreeningScreening();
		screening.setMovieId(movieSource.getMovieId());
		screening.setCinemaId(cinemaId);
		screening.setScreeningDatetime(parseDateTime(event.getEventDateTime()));
		screening.setScreeningUrl(event.getBookingLink());
		screening.setCreatedAt(LocalDateTime.now());

		screeningRepository.save(screening);
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
}
