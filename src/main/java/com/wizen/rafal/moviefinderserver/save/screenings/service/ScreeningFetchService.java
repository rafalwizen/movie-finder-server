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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningFetchService {

	private final MovieSourceScreeningRepository movieSourceRepository;
	private final ScreeningScreeningRepository screeningRepository;
	private final CinemaScreeningRepository cinemaRepository;
	private final CinemaCityApiService cinemaCityApiService;

	@Value("${screening-fetch.days-ahead:0}")
	private int daysAhead;

	private static final Long CINEMA_CITY_PROVIDER_ID = 1L;
	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	@Transactional
	public void fetchAllScreenings() {
		List<LocalDate> datesToFetch = prepareDatesList();

		log.info("Starting to fetch screenings for {} days: {}", datesToFetch.size(), datesToFetch);

		List<MovieSourceScreening> movieSources = movieSourceRepository.findByProviderId(CINEMA_CITY_PROVIDER_ID);
		log.info("Found {} movies for Cinema City provider", movieSources.size());

		int totalProcessed = 0;
		int totalScreeningsSaved = 0;
		int totalScreeningsSkipped = 0;

		for (MovieSourceScreening movieSource : movieSources) {
			try {
				ScreeningStats stats = processMovieSourceForAllDates(movieSource, datesToFetch);
				totalScreeningsSaved += stats.saved;
				totalScreeningsSkipped += stats.skipped;
				totalProcessed++;

				log.info("Processed movie {}/{}: {} ({}), saved {} screenings, skipped {} duplicates",
						totalProcessed, movieSources.size(),
						movieSource.getExternalMovieId(), movieSource.getMovieId(),
						stats.saved, stats.skipped);

				// Delay to avoid API rate limiting
				Thread.sleep(500);

			} catch (Exception e) {
				log.error("Error processing movie source {}: {}",
						movieSource.getExternalMovieId(), e.getMessage(), e);
			}
		}

		log.info("Finished fetching screenings. Processed {} movies, saved {} new screenings, skipped {} duplicates",
				totalProcessed, totalScreeningsSaved, totalScreeningsSkipped);
	}

	private List<LocalDate> prepareDatesList() {
		List<LocalDate> dates = new ArrayList<>();
		LocalDate today = LocalDate.now();

		for (int i = 0; i <= daysAhead; i++) {
			dates.add(today.plusDays(i));
		}

		return dates;
	}

	private ScreeningStats processMovieSourceForAllDates(MovieSourceScreening movieSource, List<LocalDate> dates) {
		ScreeningStats totalStats = new ScreeningStats();

		for (LocalDate date : dates) {
			try {
				ScreeningStats stats = processMovieSourceForDate(movieSource, date);
				totalStats.saved += stats.saved;
				totalStats.skipped += stats.skipped;

				// Small delay between date requests for the same movie
				Thread.sleep(200);
			} catch (Exception e) {
				log.error("Error processing movie {} for date {}: {}",
						movieSource.getExternalMovieId(), date, e.getMessage());
			}
		}

		return totalStats;
	}

	private ScreeningStats processMovieSourceForDate(MovieSourceScreening movieSource, LocalDate date) {
		CinemaCityResponse response = cinemaCityApiService.fetchScreenings(
				movieSource.getExternalMovieId(),
				date
		);

		ScreeningStats stats = new ScreeningStats();

		if (response == null || response.getBody() == null || response.getBody().getEvents() == null) {
			log.debug("No screenings found for movie {} on date {}",
					movieSource.getExternalMovieId(), date);
			return stats;
		}

		List<CinemaCityResponse.Event> events = response.getBody().getEvents();

		for (CinemaCityResponse.Event event : events) {
			try {
				boolean saved = saveScreeningIfNotExists(movieSource, event);
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

	private boolean saveScreeningIfNotExists(MovieSourceScreening movieSource, CinemaCityResponse.Event event) {
		Long cinemaId = parseCinemaId(event.getCinemaId());
		LocalDateTime screeningDateTime = parseDateTime(event.getEventDateTime());

		// Check if screening already exists
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

		// Verify cinema exists
		CinemaScreening cinema = cinemaRepository.findByIdAndProviderId(cinemaId, CINEMA_CITY_PROVIDER_ID)
				.orElseThrow(() -> new RuntimeException("Cinema not found: " + cinemaId));

		ScreeningScreening screening = new ScreeningScreening();
		screening.setMovieId(movieSource.getMovieId());
		screening.setCinemaId(cinemaId);
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
