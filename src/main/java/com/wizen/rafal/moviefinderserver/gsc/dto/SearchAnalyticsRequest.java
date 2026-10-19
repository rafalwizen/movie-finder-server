package com.wizen.rafal.moviefinderserver.gsc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for the webmasters/v3 searchAnalytics/query endpoint.
 *
 * @param dimensions null for a totals query (single aggregated row),
 *                   or e.g. ["query"] / ["page"] for a top-N breakdown
 * @param type       "web" (the current field name; "searchType" is deprecated)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SearchAnalyticsRequest(LocalDate startDate, LocalDate endDate, List<String> dimensions, Integer rowLimit, String type) {

	public static SearchAnalyticsRequest totals(LocalDate startDate, LocalDate endDate) {
		return new SearchAnalyticsRequest(startDate, endDate, null, null, "web");
	}

	public static SearchAnalyticsRequest top(LocalDate startDate, LocalDate endDate, String dimension, int rowLimit) {
		return new SearchAnalyticsRequest(startDate, endDate, List.of(dimension), rowLimit, "web");
	}
}
