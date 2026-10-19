package com.wizen.rafal.moviefinderserver.gsc.service;

import com.wizen.rafal.moviefinderserver.gsc.config.GscReportProperties;
import com.wizen.rafal.moviefinderserver.gsc.config.TelegramProperties;
import com.wizen.rafal.moviefinderserver.gsc.dto.GscReportData;
import com.wizen.rafal.moviefinderserver.gsc.dto.GscReportData.InspectionSummary;
import com.wizen.rafal.moviefinderserver.gsc.dto.UrlInspectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the daily report: search analytics for two windows, URL
 * inspections, formatting and Telegram delivery. Any failure is logged and
 * reported to Telegram when possible — the job never throws upward.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GscReportService {

	private final GscReportProperties properties;
	private final TelegramProperties telegramProperties;
	private final SearchConsoleClient searchConsoleClient;
	private final GscReportFormatter formatter;
	private final TelegramNotifier telegramNotifier;

	public void sendReport() {
		log.info("GSC report job starting: site={}, monitoredUrls={}, days={}, dataLagDays={}, zone={}, credentials={}",
				properties.getSiteUrl(), properties.getMonitoredUrls().size(), properties.getDays(),
				properties.getDataLagDays(), properties.getZone(), credentialsSource());
		try {
			doSendReport();
			log.info("GSC report sent to Telegram");
		} catch (Exception e) {
			log.error("GSC report failed", e);
			notifyFailure(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		}
	}

	private void doSendReport() {
		validateConfiguration();

		// GSC data finalizes ~2-3 days late; shift both windows back so
		// the trend comparison is never based on half-empty days.
		LocalDate end = LocalDate.now(ZoneId.of(properties.getZone())).minusDays(properties.getDataLagDays());
		int days = properties.getDays();
		LocalDate currentStart = end.minusDays(days - 1L);
		LocalDate previousEnd = end.minusDays(days);
		LocalDate previousStart = previousEnd.minusDays(days - 1L);

		GscReportData.WindowTotals current = searchConsoleClient.totals(currentStart, end);
		GscReportData.WindowTotals previous = searchConsoleClient.totals(previousStart, previousEnd);
		List<GscReportData.TopEntry> topQueries = searchConsoleClient.top(currentStart, end, "query", 5);
		List<GscReportData.TopEntry> topPages = searchConsoleClient.top(currentStart, end, "page", 5);
		List<InspectionSummary> inspections = inspectMonitoredUrls();

		GscReportData data = new GscReportData(current, previous, topQueries, topPages, inspections);
		telegramNotifier.sendHtml(formatter.format(data, properties.getSiteUrl()));
	}

	private List<InspectionSummary> inspectMonitoredUrls() {
		List<InspectionSummary> inspections = new ArrayList<>();
		for (String url : properties.getMonitoredUrls()) {
			try {
				inspections.add(toSummary(url, searchConsoleClient.inspect(url)));
			} catch (Exception e) {
				log.warn("URL inspection failed for {}", url, e);
				inspections.add(new InspectionSummary(url, null, null, null,
						e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
			}
		}
		return inspections;
	}

	private InspectionSummary toSummary(String url, UrlInspectionResponse response) {
		UrlInspectionResponse.IndexStatusResult index = response.inspectionResult() != null
				? response.inspectionResult().indexStatusResult()
				: null;
		if (index == null) {
			return new InspectionSummary(url, null, null, null, "empty inspection result");
		}
		return new InspectionSummary(url, index.verdict(), index.coverageState(), index.lastCrawlTime(), null);
	}

	private void validateConfiguration() {
		if (isBlank(telegramProperties.getBotToken())) {
			throw new IllegalStateException("telegram.bot-token is not set");
		}
		if (isBlank(telegramProperties.getChatId())) {
			throw new IllegalStateException("telegram.chat-id is not set");
		}
		if (isBlank(properties.getSiteUrl())) {
			throw new IllegalStateException("gsc.report.site-url is not set");
		}
		if (properties.getMonitoredUrls() == null || properties.getMonitoredUrls().isEmpty()) {
			throw new IllegalStateException("gsc.report.monitored-urls is empty");
		}
		if (isBlank(properties.getCredentialsJson()) == isBlank(properties.getCredentialsFile())) {
			throw new IllegalStateException("Set exactly one of gsc.report.credentials-json / gsc.report.credentials-file");
		}
	}

	private String credentialsSource() {
		if (!isBlank(properties.getCredentialsJson())) {
			return "inline-json";
		}
		if (!isBlank(properties.getCredentialsFile())) {
			return "file";
		}
		return "none";
	}

	private void notifyFailure(String reason) {
		try {
			telegramNotifier.sendHtml("⚠️ <b>Raport GSC nie powiódł się</b>\n" + GscReportFormatter.escapeHtml(reason));
		} catch (Exception e) {
			log.error("Could not deliver GSC failure notification to Telegram", e);
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
