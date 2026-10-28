package com.wizen.rafal.moviefinderserver.gsc.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Search Console API rejects non-ISO dates ("'2026' is not a valid date
 * string"). The RestTemplate's default ObjectMapper serializes java.time as
 * arrays, so the record must force string shape explicitly.
 */
class SearchAnalyticsRequestTest {

	// Mirrors the plain ObjectMapper the RestTemplate message converter uses
	// (java.time module registered, write-dates-as-timestamps left on).
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void serializesDatesAsIsoStringsEvenWithTimestampsEnabled() throws Exception {
		String json = objectMapper.writeValueAsString(SearchAnalyticsRequest.totals(
				LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16)));

		assertThat(json).contains("\"startDate\":\"2026-10-10\"");
		assertThat(json).contains("\"endDate\":\"2026-10-16\"");
		assertThat(json).doesNotContain("[2026");
	}

	@Test
	void omitsNullDimensionsAndKeepsType() throws Exception {
		String json = objectMapper.writeValueAsString(SearchAnalyticsRequest.totals(
				LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 16)));

		assertThat(json).doesNotContain("dimensions");
		assertThat(json).contains("\"type\":\"web\"");
	}
}
