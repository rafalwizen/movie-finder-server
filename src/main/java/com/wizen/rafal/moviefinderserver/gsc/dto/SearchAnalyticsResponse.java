package com.wizen.rafal.moviefinderserver.gsc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchAnalyticsResponse(List<Row> rows) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Row(List<String> keys, long clicks, long impressions, double ctr, double position) {
	}
}
