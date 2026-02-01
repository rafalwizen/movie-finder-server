package com.wizen.rafal.moviefinderserver.save.screenings.runner;

import com.wizen.rafal.moviefinderserver.save.screenings.service.ScreeningCleanupService;
import com.wizen.rafal.moviefinderserver.save.screenings.service.ScreeningFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fetch-screenings")
@RequiredArgsConstructor
@Slf4j
public class ScreeningFetchRunner implements CommandLineRunner {

	private final ScreeningFetchService screeningFetchService;
	private final ScreeningCleanupService screeningCleanupService;

	@Override
	public void run(String... args) {
		log.info("=== Starting Screening Fetch Process ===");

		try {
			// First, cleanup old screenings if enabled
			screeningCleanupService.cleanupOldScreenings();

			// Then, fetch new screenings
			screeningFetchService.fetchAllScreenings();

			log.info("=== Screening Fetch Process Completed Successfully ===");
		} catch (Exception e) {
			log.error("=== Screening Fetch Process Failed ===", e);
			System.exit(1);
		}
	}
}
