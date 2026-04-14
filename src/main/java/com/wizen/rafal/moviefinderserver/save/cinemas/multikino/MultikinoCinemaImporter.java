package com.wizen.rafal.moviefinderserver.save.cinemas.multikino;

import com.wizen.rafal.moviefinderserver.domain.model.Cinema;
import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaRepository;
import com.wizen.rafal.moviefinderserver.save.cinemas.CinemaImporter;
import com.wizen.rafal.moviefinderserver.save.cinemas.multikino.config.MultikinoCinemaConfig;
import com.wizen.rafal.moviefinderserver.save.cinemas.multikino.dto.MultikinoCinemaResponse;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MultikinoCinemaImporter implements CinemaImporter {

    private static final String PROVIDER_CODE = "MULTIKINO";
    private static final String PROVIDER_NAME = "Multikino";

    private final CinemaRepository cinemaRepository;
    private final ProviderService providerService;
    private final MultikinoCinemaConfig config;
    private final RestTemplate restTemplate;

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public void importCinemas() {
        if (!config.isEnabled()) {
            log.info("Multikino cinema import is disabled");
            return;
        }

        log.info("Starting Multikino cinema fetch from: {}", config.getUrl());

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, PROVIDER_NAME);

        try {
            MultikinoCinemaResponse response = restTemplate.getForObject(
                    config.getUrl(),
                    MultikinoCinemaResponse.class
            );

            if (response == null || response.getResult() == null) {
                log.error("Received null response from Multikino API");
                return;
            }

            List<MultikinoCinemaResponse.AlphaGroup> groups = response.getResult();
            int savedCount = 0;
            int skippedCount = 0;

            for (MultikinoCinemaResponse.AlphaGroup group : groups) {
                if (group.getCinemas() == null) continue;

                for (MultikinoCinemaResponse.CinemaDto dto : group.getCinemas()) {
                    try {
                        if (cinemaRepository.existsByProviderIdAndExternalCinemaId(
                                provider.getId(), dto.getCinemaId())) {
                            log.debug("Multikino cinema already exists: {}", dto.getCinemaName());
                            skippedCount++;
                            continue;
                        }

                        Cinema cinema = new Cinema();
                        cinema.setExternalCinemaId(dto.getCinemaId());
                        cinema.setProvider(provider);
                        cinema.setName(dto.getCinemaName());
                        cinema.setWebsiteUrl(dto.getWhatsOnUrl());
                        // Multikino API does not provide geolocation data
                        cinema.setLatitude(null);
                        cinema.setLongitude(null);
                        cinema.setCity(null);
                        cinema.setAddress(null);

                        cinemaRepository.save(cinema);
                        savedCount++;

                        log.debug("Saved Multikino cinema: {} (externalId={})",
                                cinema.getName(), cinema.getExternalCinemaId());

                    } catch (Exception e) {
                        log.error("Error processing Multikino cinema {}: {}", dto.getCinemaName(), e.getMessage());
                    }
                }
            }

            log.info("Multikino cinema fetch completed - Saved: {}, Skipped: {}", savedCount, skippedCount);

        } catch (Exception e) {
            log.error("Failed to fetch cinemas from Multikino API", e);
            throw new RuntimeException("Multikino cinema fetch failed", e);
        }
    }
}
