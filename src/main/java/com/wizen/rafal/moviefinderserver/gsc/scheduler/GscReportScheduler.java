package com.wizen.rafal.moviefinderserver.gsc.scheduler;

import com.wizen.rafal.moviefinderserver.gsc.service.GscReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "gsc.report.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class GscReportScheduler {

	private final GscReportService gscReportService;

	// The Docker container runs in UTC; zone keeps the trigger at 8:00 Warsaw time.
	@Scheduled(cron = "${gsc.report.cron:0 0 8 * * *}", zone = "${gsc.report.zone:Europe/Warsaw}")
	public void scheduledReport() {
		log.info("Scheduled GSC report triggered");
		gscReportService.sendReport();
	}
}
