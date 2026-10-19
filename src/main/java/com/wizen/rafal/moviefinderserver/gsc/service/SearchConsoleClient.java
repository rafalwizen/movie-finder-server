package com.wizen.rafal.moviefinderserver.gsc.service;

import com.wizen.rafal.moviefinderserver.gsc.config.GscReportProperties;
import com.wizen.rafal.moviefinderserver.gsc.dto.GscReportData.TopEntry;
import com.wizen.rafal.moviefinderserver.gsc.dto.GscReportData.WindowTotals;
import com.wizen.rafal.moviefinderserver.gsc.dto.InspectionRequest;
import com.wizen.rafal.moviefinderserver.gsc.dto.SearchAnalyticsRequest;
import com.wizen.rafal.moviefinderserver.gsc.dto.SearchAnalyticsResponse;
import com.wizen.rafal.moviefinderserver.gsc.dto.UrlInspectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Thin client for the Google Search Console API: search analytics totals/tops
 * and URL inspection. Uses the shared RestTemplate with a Bearer token from
 * the service account.
 */
@Component
@RequiredArgsConstructor
public class SearchConsoleClient {

	private static final String SEARCH_ANALYTICS_URL = "https://searchconsole.googleapis.com/webmasters/v3/sites/%s/searchAnalytics/query";
	private static final String URL_INSPECTION_URL = "https://searchconsole.googleapis.com/v1/urlInspection/index:inspect";

	private final RestTemplate restTemplate;
	private final GoogleAccessTokenProvider accessTokenProvider;
	private final GscReportProperties properties;

	/**
	 * Aggregated metrics for a date window. A no-dimension query returns a
	 * single row (null rows means no data at all for the window).
	 */
	public WindowTotals totals(LocalDate start, LocalDate end) {
		SearchAnalyticsResponse response = post(
				SEARCH_ANALYTICS_URL.formatted(encodedSiteUrl()),
				SearchAnalyticsRequest.totals(start, end),
				SearchAnalyticsResponse.class);
		List<SearchAnalyticsResponse.Row> rows = response.rows();
		if (rows == null || rows.isEmpty()) {
			return new WindowTotals(start, end, 0, 0, 0, 0);
		}
		SearchAnalyticsResponse.Row row = rows.get(0);
		return new WindowTotals(start, end, row.clicks(), row.impressions(), row.ctr(), row.position());
	}

	/**
	 * Top-N entries for one dimension ("query" or "page"), best first.
	 */
	public List<TopEntry> top(LocalDate start, LocalDate end, String dimension, int limit) {
		SearchAnalyticsResponse response = post(
				SEARCH_ANALYTICS_URL.formatted(encodedSiteUrl()),
				SearchAnalyticsRequest.top(start, end, dimension, limit),
				SearchAnalyticsResponse.class);
		List<SearchAnalyticsResponse.Row> rows = response.rows();
		if (rows == null) {
			return List.of();
		}
		return rows.stream()
				.map(row -> new TopEntry(
						row.keys() == null || row.keys().isEmpty() ? "?" : row.keys().get(0),
						row.clicks(), row.impressions(), row.ctr(), row.position()))
				.toList();
	}

	public UrlInspectionResponse inspect(String inspectionUrl) {
		// siteUrl goes raw (un-encoded) in the body of this endpoint
		return post(URL_INSPECTION_URL, new InspectionRequest(inspectionUrl, properties.getSiteUrl(), "pl"), UrlInspectionResponse.class);
	}

	/**
	 * The webmasters/v3 endpoint expects the site URL URL-encoded as a path
	 * segment, e.g. https%3A%2F%2Fgdzie-do-kina.pl%2F. The URI is passed as a
	 * java.net.URI so RestTemplate does not re-encode or template-expand it.
	 */
	private String encodedSiteUrl() {
		return URLEncoder.encode(properties.getSiteUrl(), StandardCharsets.UTF_8);
	}

	private <T> T post(String url, Object body, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			headers.setBearerAuth(accessTokenProvider.getAccessToken());
		} catch (IOException e) {
			throw new IllegalStateException("Failed to obtain Google access token: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Failed to obtain Google access token: " + e.getMessage(), e);
		}
		try {
			T response = restTemplate.postForObject(URI.create(url), new HttpEntity<>(body, headers), responseType);
			if (response == null) {
				throw new IllegalStateException("Google API returned empty response body");
			}
			return response;
		} catch (RestClientResponseException e) {
			throw new IllegalStateException("Google API call failed: HTTP " + e.getStatusCode().value() + " " + e.getResponseBodyAsString(), e);
		}
	}
}
