package com.wizen.rafal.moviefinderserver.gsc.runner;

import com.wizen.rafal.moviefinderserver.gsc.service.GscReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-shot manual trigger for the GSC report, independent of
 * gsc.report.enabled (mirrors the fetch-* runner profiles).
 */
@Component
@Profile("gsc-report")
@RequiredArgsConstructor
@Slf4j
public class GscReportRunner implements CommandLineRunner {

	private final GscReportService gscReportService;

	@Override
	public void run(String... args) {
		log.info("Manual GSC report run (profile gsc-report)");
		gscReportService.sendReport();
	}
}
