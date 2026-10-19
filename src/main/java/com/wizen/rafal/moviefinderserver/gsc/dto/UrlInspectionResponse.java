package com.wizen.rafal.moviefinderserver.gsc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UrlInspectionResponse(InspectionResult inspectionResult) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InspectionResult(String inspectionResultLink, String verdict, IndexStatusResult indexStatusResult) {
	}

	/**
	 * verdict: PASS (indexed) / NEUTRAL (excluded from index) / FAIL (error);
	 * coverageState carries the human-readable state, e.g. "Indexed".
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record IndexStatusResult(String verdict, String coverageState, String pageFetchState, String indexingState, String robotsTxtState, String lastCrawlTime) {
	}
}
