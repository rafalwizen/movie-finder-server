package com.wizen.rafal.moviefinderserver.gsc.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated input for the report formatter, already stripped of HTTP concerns.
 */
public record GscReportData(WindowTotals current, WindowTotals previous, List<TopEntry> topQueries, List<TopEntry> topPages, List<InspectionSummary> inspections) {

	public record WindowTotals(LocalDate start, LocalDate end, long clicks, long impressions, double ctr, double position) {
	}

	public record TopEntry(String key, long clicks, long impressions, double ctr, double position) {
	}

	public record InspectionSummary(String url, String verdict, String coverageState, String lastCrawlTime, String error) {
	}
}
