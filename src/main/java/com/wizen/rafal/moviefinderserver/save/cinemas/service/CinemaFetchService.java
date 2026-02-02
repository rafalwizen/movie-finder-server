package com.wizen.rafal.moviefinderserver.save.cinemas.service;

import com.wizen.rafal.moviefinderserver.save.cinemas.config.CinemaFetchProperties;
import com.wizen.rafal.moviefinderserver.save.cinemas.dto.CinemaCityResponse;
import com.wizen.rafal.moviefinderserver.domain.model.Cinema;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class CinemaFetchService {

	private static final Logger log = LoggerFactory.getLogger(CinemaFetchService.class);

	private final CinemaRepository cinemaRepository;
	private final CinemaFetchProperties properties;
	private final RestTemplate restTemplate;

	public CinemaFetchService(CinemaRepository cinemaRepository,
							  CinemaFetchProperties properties,
							  RestTemplate restTemplate) {
		this.cinemaRepository = cinemaRepository;
		this.properties = properties;
		this.restTemplate = restTemplate;
	}

	public void fetchAndSaveCinemas() {
		if (!properties.isEnabled()) {
			log.info("Cinema fetch is disabled");
			return;
		}

		log.info("Starting cinema fetch from: {}", properties.getUrl());

		try {
			CinemaCityResponse response = restTemplate.getForObject(
					properties.getUrl(),
					CinemaCityResponse.class
			);

			if (response == null || response.getBody() == null) {
				log.error("Received null response from Cinema City API");
				return;
			}

			List<CinemaCityResponse.CinemaDto> cinemaDtos = response.getBody().getCinemas();

			if (cinemaDtos == null || cinemaDtos.isEmpty()) {
				log.warn("No cinemas found in response");
				return;
			}

			log.info("Found {} cinemas in API response", cinemaDtos.size());

			int savedCount = 0;
			int skippedCount = 0;
			List<String> errors = new ArrayList<>();

			for (CinemaCityResponse.CinemaDto dto : cinemaDtos) {
				try {
					String externalCinemaId = dto.getId();

					if (externalCinemaId == null || externalCinemaId.trim().isEmpty()) {
						errors.add("Invalid cinema ID: " + dto.getDisplayName());
						continue;
					}

					if (cinemaRepository.existsByProviderIdAndExternalCinemaId(
							properties.getProviderId(), externalCinemaId)) {
						log.debug("Cinema already exists: externalCinemaId={}, providerId={}",
								externalCinemaId, properties.getProviderId());
						skippedCount++;
						continue;
					}

					Cinema cinema = mapToCinema(dto, externalCinemaId);
					cinema = cinemaRepository.save(cinema);
					savedCount++;

					log.debug("Saved cinema: {} (id={}, externalId={}, city={})",
							cinema.getName(), cinema.getId(), cinema.getExternalCinemaId(), cinema.getCity());

				} catch (Exception e) {
					String errorMsg = String.format("Error processing cinema %s: %s",
							dto.getDisplayName(), e.getMessage());
					errors.add(errorMsg);
					log.error(errorMsg, e);
				}
			}

			log.info("Cinema fetch completed - Saved: {}, Skipped: {}, Errors: {}",
					savedCount, skippedCount, errors.size());

			if (!errors.isEmpty()) {
				log.warn("Errors encountered during fetch:");
				errors.forEach(log::warn);
			}

		} catch (Exception e) {
			log.error("Failed to fetch cinemas from Cinema City API", e);
			throw new RuntimeException("Cinema fetch failed", e);
		}
	}

	private Cinema mapToCinema(CinemaCityResponse.CinemaDto dto, String externalCinemaId) {
		Cinema cinema = new Cinema();
		cinema.setExternalCinemaId(externalCinemaId);
		cinema.setProviderId(properties.getProviderId());
		cinema.setName(dto.getDisplayName());
		cinema.setWebsiteUrl(dto.getLink());
		cinema.setLatitude(dto.getLatitude());
		cinema.setLongitude(dto.getLongitude());

		if (dto.getAddressInfo() != null) {
			cinema.setCity(dto.getAddressInfo().getCity());
			cinema.setAddress(dto.getAddressInfo().getFullAddress());
		}

		return cinema;
	}
}
