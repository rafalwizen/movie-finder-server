package com.wizen.rafal.moviefinderserver.save.movies.helios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wizen.rafal.moviefinderserver.domain.model.Cinema;
import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import com.wizen.rafal.moviefinderserver.domain.model.MovieSource;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieSourceRepository;
import com.wizen.rafal.moviefinderserver.save.common.service.HeliosPageFetcher;
import com.wizen.rafal.moviefinderserver.save.common.service.ProviderService;
import com.wizen.rafal.moviefinderserver.save.movies.FilmImporter;
import com.wizen.rafal.moviefinderserver.save.movies.helios.config.HeliosFilmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeliosFilmImporter implements FilmImporter {

    private static final String PROVIDER_CODE = "HELIOS";
    private static final String PROVIDER_NAME = "Helios";
    private static final Pattern FILM_HREF_PATTERN =
            Pattern.compile("/filmy/([^/]+-(\\d+))$");
    private static final Pattern ISO_DURATION_PATTERN =
            Pattern.compile("PT(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+)M)?");

    private final HeliosFilmConfig config;
    private final ProviderService providerService;
    private final MovieRepository movieRepository;
    private final MovieSourceRepository movieSourceRepository;
    private final CinemaRepository cinemaRepository;
    private final HeliosPageFetcher pageFetcher;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    @Override
    @Transactional
    public void importFilms() {
        if (!config.isEnabled()) {
            log.info("Helios film import is disabled");
            return;
        }

        log.info("Starting Helios film download");

        CinemaProvider provider = providerService.getOrCreateProvider(PROVIDER_CODE, PROVIDER_NAME);

        try {
            List<Cinema> cinemas = cinemaRepository.findByProviderId(provider.getId());

            if (config.getCinemaLimit() > 0 && cinemas.size() > config.getCinemaLimit()) {
                log.info("Limiting to {} cinemas (config limit)", config.getCinemaLimit());
                cinemas = cinemas.subList(0, config.getCinemaLimit());
            }

            int totalAdded = 0;
            int totalSkipped = 0;

            for (Cinema cinema : cinemas) {
                try {
                    String repertoireUrl = config.getBaseUrl() + "/" + cinema.getExternalCinemaId() + "/repertuar";
                    Document doc = pageFetcher.fetchPage(repertoireUrl, config.getRateLimitMs());

                    Elements filmGroups = doc.select("[aria-labelledby^=repertoire-screening-movie-]");
                    log.info("Found {} film groups for cinema {}", filmGroups.size(), cinema.getName());

                    for (Element group : filmGroups) {
                        try {
                            ParsedFilmLink parsed = parseFilmLink(group);
                            if (parsed == null) continue;

                            Optional<MovieSource> existing = movieSourceRepository
                                    .findByProviderIdAndExternalMovieId(provider.getId(), parsed.externalId);

                            if (existing.isPresent()) {
                                totalSkipped++;
                                continue;
                            }

                            FilmData filmData = extractFilmData(group, parsed.title);

                            if (config.isFetchFilmDetails()) {
                                enrichFilmDetails(filmData, cinema.getExternalCinemaId(), parsed.filmPath);
                            }

                            Long newMovieId = providerService.nextMovieId();
                            Movie movie = Movie.builder()
                                    .id(newMovieId)
                                    .title(filmData.title)
                                    .originalTitle(filmData.originalTitle)
                                    .posterUrl(filmData.posterUrl)
                                    .durationMinutes(filmData.durationMinutes)
                                    .description(filmData.description)
                                    .year(filmData.year)
                                    .build();
                            movieRepository.save(movie);

                            Long newSourceId = providerService.nextMovieSourceId();
                            MovieSource movieSource = MovieSource.builder()
                                    .id(newSourceId)
                                    .movieId(movie.getId())
                                    .providerId(provider.getId())
                                    .externalMovieId(parsed.externalId)
                                    .build();
                            movieSourceRepository.save(movieSource);

                            log.debug("Added Helios film: {} (external ID: {})", movie.getTitle(), parsed.externalId);
                            totalAdded++;

                        } catch (Exception e) {
                            log.error("Error processing film group: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error fetching repertoire for cinema {}: {}", cinema.getName(), e.getMessage());
                }
            }

            log.info("Helios film download completed. Added: {}, Skipped: {}", totalAdded, totalSkipped);

        } catch (Exception e) {
            log.error("Failed to download films from Helios", e);
            throw new RuntimeException("Helios film download failed", e);
        }
    }

    private String extractFilmId(Element group) {
        ParsedFilmLink parsed = parseFilmLink(group);
        return parsed != null ? parsed.externalId : null;
    }

    private ParsedFilmLink parseFilmLink(Element group) {
        Elements links = group.select("a[href*=/filmy/]");
        for (Element link : links) {
            Matcher matcher = FILM_HREF_PATTERN.matcher(link.attr("href"));
            if (matcher.find()) {
                String filmPath = matcher.group(1);
                String externalId = matcher.group(2);
                return new ParsedFilmLink(externalId, filmPath, link.text().trim());
            }
        }
        return null;
    }

    private FilmData extractFilmData(Element group, String title) {
        FilmData data = new FilmData();
        data.title = title;

        Element img = group.selectFirst("img[src*=helios.pl]");
        if (img != null) {
            data.posterUrl = img.attr("src");
        }

        return data;
    }

    private void enrichFilmDetails(FilmData data, String cinemaExternalId, String filmPath) {
        try {
            String filmUrl = config.getBaseUrl() + "/" + cinemaExternalId + "/filmy/" + filmPath;
            Document doc = pageFetcher.fetchPage(filmUrl, config.getRateLimitMs());

            Elements scripts = doc.select("script[type=application/ld+json]");
            for (Element script : scripts) {
                JsonNode node = objectMapper.readTree(script.html());
                if (node.has("@type") && "Movie".equals(node.get("@type").asText())) {
                    applyMovieJsonLd(data, node);
                    return;
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch film details for '{}': {}", data.title, e.getMessage());
        }
    }

    private void applyMovieJsonLd(FilmData data, JsonNode node) {
        if (node.has("name")) {
            data.title = node.get("name").asText();
        }
        if (node.has("description")) {
            data.description = node.get("description").asText();
        }
        if (node.has("dateCreated")) {
            String dateStr = node.get("dateCreated").asText();
            try {
                data.year = Integer.parseInt(dateStr.substring(0, 4));
            } catch (NumberFormatException ignored) {}
        }
        if (node.has("duration")) {
            data.durationMinutes = parseIsoDuration(node.get("duration").asText());
        }
        if (node.has("image")) {
            data.posterUrl = node.get("image").asText();
        }
    }

    private Integer parseIsoDuration(String iso) {
        Matcher m = ISO_DURATION_PATTERN.matcher(iso);
        if (!m.find()) return null;
        double totalMinutes = 0;
        if (m.group(1) != null) {
            totalMinutes += Double.parseDouble(m.group(1)) * 60;
        }
        if (m.group(2) != null) {
            totalMinutes += Integer.parseInt(m.group(2));
        }
        return totalMinutes > 0 ? (int) Math.round(totalMinutes) : null;
    }

    private static class FilmData {
        String title;
        String originalTitle;
        String posterUrl;
        String description;
        Integer year;
        Integer durationMinutes;
    }

    private record ParsedFilmLink(String externalId, String filmPath, String title) {}
}
