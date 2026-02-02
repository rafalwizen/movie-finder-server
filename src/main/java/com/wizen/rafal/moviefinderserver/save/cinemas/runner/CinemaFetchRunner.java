package com.wizen.rafal.moviefinderserver.save.cinemas.runner;

import com.wizen.rafal.moviefinderserver.save.cinemas.service.CinemaFetchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("fetch-cinemas")
public class CinemaFetchRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(CinemaFetchRunner.class);

	private final CinemaFetchService cinemaFetchService;

	public CinemaFetchRunner(CinemaFetchService cinemaFetchService) {
		this.cinemaFetchService = cinemaFetchService;
	}

	@Override
	public void run(String... args) {
		log.info("Starting cinema fetch process...");

		try {
			cinemaFetchService.fetchAndSaveCinemas();
			log.info("Cinema fetch process completed successfully");
		} catch (Exception e) {
			log.error("Cinema fetch process failed", e);
			System.exit(1);
		}
	}
}
