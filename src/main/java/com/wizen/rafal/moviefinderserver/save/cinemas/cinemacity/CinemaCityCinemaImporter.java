package com.wizen.rafal.moviefinderserver.save.cinemas.cinemacity;

import com.wizen.rafal.moviefinderserver.domain.model.Cinema;
import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaProviderRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaRepository;
import com.wizen.rafal.moviefinderserver.save.cinemas.CinemaImporter;
import com.wizen.rafal.moviefinderserver.save.cinemas.cinemacity.config.CinemaCityCinemaConfig;
import com.wizen.rafal.moviefinderserver.save.cinemas.cinemacity.dto.CinemaCityCinemaResponse;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CinemaCityCinemaImporter implements CinemaImporter {

    private static final String PROVIDER_CODE = "CINEMA_CITY";

    private final CinemaRepository cinemaRepository;
    private final ProviderService providerService;
    private final CinemaCityCinemaConfig config;
    private final RestTemplate restTemplate;

    @Override
    public void importCinemas() {
        if (!config.isEnabled()) {
            log.info("Cinema City cinema import is disabled");
            return;
        }

        log.info("Starting Cinema City cinema fetch from: {}", config.getUrl());

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, "Cinema City");

        try {
            CinemaCityCinemaResponse response = restTemplate.getForObject(
                    config.getUrl(),
                    CinemaCityCinemaResponse.class
            );

            if (response == null || response.getBody() == null) {
                log.error("Received null response from Cinema City API");
                return;
            }

            List<CinemaCityCinemaResponse.CinemaDto> cinemaDtos = response.getBody().getCinemas();

            if (cinemaDtos == null || cinemaDtos.isEmpty()) {
                log.warn("No cinemas found in response");
                return;
            }

            log.info("Found {} cinemas in API response", cinemaDtos.size());

            int savedCount = 0;
            int skippedCount = 0;
            List<String> errors = new ArrayList<>();

            for (CinemaCityCinemaResponse.CinemaDto dto : cinemaDtos) {
                try {
                    String externalCinemaId = dto.getId();

                    if (externalCinemaId == null || externalCinemaId.trim().isEmpty()) {
                        errors.add("Invalid cinema ID: " + dto.getDisplayName());
                        continue;
                    }

                    if (cinemaRepository.existsByProviderIdAndExternalCinemaId(
                            provider.getId(), externalCinemaId)) {
                        log.debug("Cinema already exists: externalCinemaId={}, providerId={}",
                                externalCinemaId, provider.getId());
                        skippedCount++;
                        continue;
                    }

                    Cinema cinema = mapToCinema(dto, externalCinemaId, provider);
                    cinemaRepository.save(cinema);
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

            log.info("Cinema City cinema fetch completed - Saved: {}, Skipped: {}, Errors: {}",
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

    private Cinema mapToCinema(CinemaCityCinemaResponse.CinemaDto dto, String externalCinemaId, CinemaProvider provider) {
        Cinema cinema = new Cinema();
        cinema.setExternalCinemaId(externalCinemaId);
        cinema.setProvider(provider);
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
