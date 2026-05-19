package com.wizen.rafal.moviefinderserver.save.cinemas.helios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wizen.rafal.moviefinderserver.domain.model.Cinema;
import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaRepository;
import com.wizen.rafal.moviefinderserver.save.cinemas.CinemaImporter;
import com.wizen.rafal.moviefinderserver.save.cinemas.helios.config.HeliosCinemaConfig;
import com.wizen.rafal.moviefinderserver.save.common.service.HeliosPageFetcher;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeliosCinemaImporter implements CinemaImporter {

    private static final String PROVIDER_CODE = "HELIOS";
    private static final String PROVIDER_NAME = "Helios";
    private static final Pattern CINEMA_HREF_PATTERN =
            Pattern.compile("^/([a-z0-9-]+/(?:kino-helios[a-z0-9-]*))$");

    private final CinemaRepository cinemaRepository;
    private final ProviderService providerService;
    private final HeliosCinemaConfig config;
    private final HeliosPageFetcher pageFetcher;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    public void importCinemas() {
        if (!config.isEnabled()) {
            log.info("Helios cinema import is disabled");
            return;
        }

        log.info("Starting Helios cinema fetch");

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, PROVIDER_NAME);

        try {
            Document mainPage = pageFetcher.fetchPage(config.getUrl(), config.getRateLimitMs());
            Set<CinemaEntry> cinemas = parseCinemaList(mainPage);
            log.info("Found {} Helios cinemas in dropdown", cinemas.size());

            int savedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;

            for (CinemaEntry entry : cinemas) {
                try {
                    if (cinemaRepository.existsByProviderIdAndExternalCinemaId(
                            provider.getId(), entry.externalId)) {
                        log.debug("Helios cinema already exists: {}", entry.name);
                        skippedCount++;
                        continue;
                    }

                    Cinema cinema = new Cinema();
                    cinema.setExternalCinemaId(entry.externalId);
                    cinema.setProvider(provider);
                    cinema.setName(entry.name);
                    cinema.setCity(entry.city);
                    cinema.setWebsiteUrl("https://helios.pl/" + entry.externalId);

                    if (config.isFetchDetails()) {
                        enrichCinemaDetails(cinema, entry.externalId);
                    }

                    cinemaRepository.save(cinema);
                    savedCount++;
                    log.debug("Saved Helios cinema: {} ({})", cinema.getName(), cinema.getCity());

                } catch (Exception e) {
                    log.error("Error processing Helios cinema {}: {}", entry.name, e.getMessage());
                    errorCount++;
                }
            }

            log.info("Helios cinema fetch completed - Saved: {}, Skipped: {}, Errors: {}",
                    savedCount, skippedCount, errorCount);

        } catch (Exception e) {
            log.error("Failed to fetch Helios cinemas", e);
            throw new RuntimeException("Helios cinema fetch failed", e);
        }
    }

    private Set<CinemaEntry> parseCinemaList(Document doc) {
        Set<CinemaEntry> cinemas = new LinkedHashSet<>();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("href");
            Matcher matcher = CINEMA_HREF_PATTERN.matcher(href);
            if (!matcher.matches()) {
                continue;
            }

            String externalId = matcher.group(1);
            String city = extractCityFromElement(link);
            String cinemaSuffix = extractCinemaNameFromElement(link);

            cinemas.add(new CinemaEntry(
                    externalId,
                    city,
                    "Kino Helios " + cinemaSuffix
            ));
        }

        return cinemas;
    }

    private String extractCityFromElement(Element link) {
        Element strong = link.selectFirst("strong");
        if (strong != null) {
            return strong.text().trim();
        }
        return "";
    }

    private String extractCinemaNameFromElement(Element link) {
        String fullText = link.text().trim();
        int dashIdx = fullText.indexOf('-');
        if (dashIdx >= 0) {
            return fullText.substring(dashIdx + 1).trim();
        }
        return fullText;
    }

    private void enrichCinemaDetails(Cinema cinema, String externalId) {
        try {
            String url = "https://helios.pl/" + externalId;
            Document cinemaPage = pageFetcher.fetchPage(url, config.getRateLimitMs());

            Elements scripts = cinemaPage.select("script[type=application/ld+json]");
            for (Element script : scripts) {
                JsonNode node = objectMapper.readTree(script.html());
                if (node.has("@type") && "LocalBusiness".equals(node.get("@type").asText())) {
                    applyCinemaJsonLd(cinema, node);
                    return;
                }
            }
            log.warn("No LocalBusiness JSON-LD found for cinema: {}", externalId);
        } catch (Exception e) {
            log.warn("Failed to fetch details for cinema {}: {}", externalId, e.getMessage());
        }
    }

    private void applyCinemaJsonLd(Cinema cinema, JsonNode node) {
        if (node.has("address")) {
            JsonNode address = node.get("address");
            if (address.has("streetAddress")) {
                cinema.setAddress(address.get("streetAddress").asText());
            }
        }
        if (node.has("geo")) {
            JsonNode geo = node.get("geo");
            if (geo.has("latitude")) {
                cinema.setLatitude(geo.get("latitude").asDouble());
            }
            if (geo.has("longitude")) {
                cinema.setLongitude(geo.get("longitude").asDouble());
            }
        }
        if (node.has("name") && cinema.getName() == null) {
            cinema.setName(node.get("name").asText());
        }
    }

    private record CinemaEntry(String externalId, String city, String name) {}
}
