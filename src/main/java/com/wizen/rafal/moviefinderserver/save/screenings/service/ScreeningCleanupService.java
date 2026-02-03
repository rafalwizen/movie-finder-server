package com.wizen.rafal.moviefinderserver.save.screenings.service;

import com.wizen.rafal.moviefinderserver.save.screenings.config.ScreeningFetchProperties;
import com.wizen.rafal.moviefinderserver.domain.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningCleanupService {

	private final ScreeningRepository screeningRepository;
	private final ScreeningFetchProperties properties;

	@Transactional
	public void cleanupOldScreenings() {
		if (!properties.getCleanup().isEnabled()) {
			log.info("Screening cleanup is disabled in configuration");
			return;
		}

		Integer daysToKeep = properties.getCleanup().getDaysToKeep();

		if (daysToKeep == null) {
			log.info("Cleanup days-to-keep is not set, skipping cleanup");
			return;
		}

		if (daysToKeep < 0) {
			log.warn("Invalid days-to-keep value: {}. Must be 0 or positive number. Skipping cleanup.", daysToKeep);
			return;
		}

		LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(daysToKeep);

		log.info("Starting cleanup of screenings older than {} days (before {})",
				daysToKeep, cutoffDateTime);

		int deletedCount = screeningRepository.deleteByScreeningDatetimeBefore(cutoffDateTime);

		log.info("Cleanup completed. Deleted {} old screenings", deletedCount);
	}
}
