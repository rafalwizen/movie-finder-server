package com.wizen.rafal.moviefinderserver.gsc.service;

import com.wizen.rafal.moviefinderserver.gsc.dto.GscReportData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GscReportFormatterTest {

	private final GscReportFormatter formatter = new GscReportFormatter();

	@Test
	void formatsFullReportWithTrendsAndEscapesDynamicValues() {
		GscReportData data = new GscReportData(
				new GscReportData.WindowTotals(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16), 120, 5400, 0.0222, 12.4),
				new GscReportData.WindowTotals(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 9), 100, 6000, 0.0167, 13.2),
				List.of(new GscReportData.TopEntry("kino <f&c> lublin", 45, 1200, 0.037, 3.1)),
				List.of(new GscReportData.TopEntry("https://gdzie-do-kina.pl/movies/12", 98, 2100, 0.046, 5.2)),
				List.of(new GscReportData.InspectionSummary("https://gdzie-do-kina.pl/", "PASS", "Indexed", "2026-10-15T07:26:40Z", null)));

		String html = formatter.format(data, "https://gdzie-do-kina.pl/");

		assertThat(html).contains("Raport GSC — gdzie-do-kina.pl");
		assertThat(html).contains("120"); // current clicks
		assertThat(html).contains("▲");   // clicks up 100 -> 120
		assertThat(html).contains("kino &lt;f&amp;c&gt; lublin"); // escaped query
		assertThat(html).contains("href=\"https://gdzie-do-kina.pl/movies/12\"");
		assertThat(html).contains("Zaindeksowana");
		assertThat(html).contains("crawl: 2026-10-15");
		assertThat(html).contains("lepiej"); // position 13.2 -> 12.4 improved
	}

	@Test
	void handlesEmptyWindowsAndInspectionError() {
		GscReportData data = new GscReportData(
				new GscReportData.WindowTotals(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16), 0, 0, 0, 0),
				new GscReportData.WindowTotals(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 9), 0, 0, 0, 0),
				List.of(), List.of(),
				List.of(new GscReportData.InspectionSummary("https://gdzie-do-kina.pl/", null, null, null, "boom")));

		String html = formatter.format(data, "https://gdzie-do-kina.pl/");

		assertThat(html).contains("brak danych");
		assertThat(html).contains("bez zmian");
		assertThat(html).contains("błąd: boom");
	}
}
