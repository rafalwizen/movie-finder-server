package com.wizen.rafal.moviefinderserver.save.screenings.service;


import com.wizen.rafal.moviefinderserver.save.screenings.dto.CinemaCityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinemaCityApiService {

	@Autowired
	@Qualifier("restTemplate")
//	@Qualifier("screeningsRestTemplate")
	private RestTemplate restTemplate;

	private static final String API_URL_TEMPLATE =
			"https://www.cinema-city.pl/pl/data-api-service/v1/quickbook/10103/cinema-events/in-group/katowice/with-film/%s/at-date/%s?attr=&lang=pl_PL";

	public CinemaCityResponse fetchScreenings(String externalMovieId, LocalDate date) {
		String formattedDate = date.format(DateTimeFormatter.ISO_DATE);
		String url = String.format(API_URL_TEMPLATE, externalMovieId, formattedDate);

		log.info("Fetching screenings for movie {} on date {}", externalMovieId, formattedDate);

		try {
			return restTemplate.getForObject(url, CinemaCityResponse.class);
		} catch (Exception e) {
			log.error("Error fetching screenings for movie {} on date {}: {}",
					externalMovieId, formattedDate, e.getMessage());
			return null;
		}
	}
}
