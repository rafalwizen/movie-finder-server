package com.wizen.rafal.moviefinderserver.save.screenings.helios;

import com.wizen.rafal.moviefinderserver.domain.model.*;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieSourceRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.ScreeningRepository;
import com.wizen.rafal.moviefinderserver.save.common.service.HeliosPageFetcher;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import com.wizen.rafal.moviefinderserver.save.screenings.ScreeningImporter;
import com.wizen.rafal.moviefinderserver.save.screenings.helios.config.HeliosScreeningConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeliosScreeningImporter implements ScreeningImporter {

    private static final String PROVIDER_CODE = "HELIOS";
    private static final String PROVIDER_NAME = "Helios";
    private static final Pattern ITEM_SOURCE_ID_PATTERN =
            Pattern.compile("item_source_id=(\\d+)");

    private final HeliosScreeningConfig config;
    private final ProviderService providerService;
    private final MovieSourceRepository movieSourceRepository;
    private final ScreeningRepository screeningRepository;
    private final CinemaRepository cinemaRepository;
    private final HeliosPageFetcher pageFetcher;

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    @Transactional
    public void importScreenings() {
        if (!config.isEnabled()) {
            log.info("Helios screening import is disabled");
            return;
        }

        log.info("Starting Helios screening fetch");

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, PROVIDER_NAME);

        try {
            List<Cinema> cinemas = cinemaRepository.findByProviderId(provider.getId());

            if (config.getCinemaLimit() > 0 && cinemas.size() > config.getCinemaLimit()) {
                log.info("Limiting to {} cinemas (config limit)", config.getCinemaLimit());
                cinemas = cinemas.subList(0, config.getCinemaLimit());
            }

            List<MovieSource> sources = movieSourceRepository.findByProviderId(provider.getId());
            Map<String, MovieSource> sourceByExternalId = sources.stream()
                    .collect(Collectors.toMap(MovieSource::getExternalMovieId, Function.identity()));

            int totalAdded = 0;
            int totalSkipped = 0;
            int totalErrors = 0;

            for (Cinema cinema : cinemas) {
                try {
                    String repertoireUrl = config.getBaseUrl() + "/" + cinema.getExternalCinemaId() + "/repertuar";
                    Document doc = pageFetcher.fetchPage(repertoireUrl, config.getRateLimitMs());

                    Elements screeningLinks = doc.select("a[href*=bilety.helios.pl]");
                    log.info("Found {} screening links for cinema {}", screeningLinks.size(), cinema.getName());

                    for (Element link : screeningLinks) {
                        try {
                            String href = link.attr("href");
                            String externalMovieId = extractItemSourceId(href);
                            if (externalMovieId == null) {
                                log.debug("Could not extract item_source_id from URL: {}", href);
                                continue;
                            }

                            MovieSource movieSource = sourceByExternalId.get(externalMovieId);
                            if (movieSource == null) {
                                log.debug("No MovieSource found for external ID: {}", externalMovieId);
                                continue;
                            }

                            Element timeElement = link.selectFirst("time[datetime]");
                            if (timeElement == null) {
                                log.debug("No time element found in screening link");
                                continue;
                            }

                            LocalDateTime screeningTime = parseDateTime(timeElement.attr("datetime"));
                            if (screeningTime == null) continue;

                            if (screeningRepository.existsByMovieIdAndCinemaIdAndScreeningDatetime(
                                    movieSource.getMovieId(), cinema.getId(), screeningTime)) {
                                totalSkipped++;
                                continue;
                            }

                            Screening screening = new Screening();
                            screening.setMovie(movieSource.getMovie());
                            screening.setCinema(cinema);
                            screening.setScreeningDatetime(screeningTime);
                            screening.setScreeningUrl(href.replace("&amp;", "&"));
                            screeningRepository.save(screening);

                            totalAdded++;

                        } catch (Exception e) {
                            log.error("Error processing screening: {}", e.getMessage());
                            totalErrors++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error fetching screenings for cinema {}: {}", cinema.getName(), e.getMessage());
                }
            }

            log.info("Helios screening fetch completed. Added: {}, Skipped: {}, Errors: {}",
                    totalAdded, totalSkipped, totalErrors);

        } catch (Exception e) {
            log.error("Failed to fetch screenings from Helios", e);
            throw new RuntimeException("Helios screening fetch failed", e);
        }
    }

    private String extractItemSourceId(String url) {
        Matcher matcher = ITEM_SOURCE_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private LocalDateTime parseDateTime(String datetime) {
        try {
            return OffsetDateTime.parse(datetime).toLocalDateTime();
        } catch (Exception e) {
            log.debug("Could not parse datetime: {}", datetime);
            return null;
        }
    }
}
