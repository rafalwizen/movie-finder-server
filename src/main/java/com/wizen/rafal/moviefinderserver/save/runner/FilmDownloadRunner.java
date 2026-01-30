package com.wizen.rafal.moviefinderserver.save.runner;

import com.wizen.rafal.moviefinderserver.save.service.FilmDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("download-films")
@RequiredArgsConstructor
public class FilmDownloadRunner implements CommandLineRunner {

	private final FilmDownloadService filmDownloadService;

	@Override
	public void run(String... args) {
		log.info("=== URUCHOMIONO PROFIL: download-films ===");

		try {
			filmDownloadService.downloadAndSaveFilms();
			log.info("=== ZAKOŃCZONO POBIERANIE FILMÓW ===");
		} catch (Exception e) {
			log.error("Wystąpił błąd podczas pobierania filmów", e);
		}

		// Możesz zdecydować czy aplikacja ma się zakończyć po pobraniu:
		// System.exit(0);
	}
}
