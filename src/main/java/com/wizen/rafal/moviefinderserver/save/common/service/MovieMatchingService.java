package com.wizen.rafal.moviefinderserver.save.common.service;

import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Finds existing Movie records that represent the same real-world film
 * across different cinema providers. Uses title normalization and
 * fuzzy matching (exact → prefix → Levenshtein) with year disambiguation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MovieMatchingService {

    private static final double SIMILARITY_THRESHOLD = 0.82;
    private static final double PREFIX_LENGTH_RATIO = 0.65;
    private static final double PREFIX_MATCH_SCORE = 0.85;
    private static final double LENGTH_RATIO_LIMIT = 1.5;

    private final MovieRepository movieRepository;

    /**
     * Searches all existing movies for one that represents the same real-world film.
     * Compares both title and originalTitle fields with fuzzy matching.
     *
     * @return the best matching Movie above threshold, or empty if no confident match
     */
    public Optional<Movie> findMatchingMovie(String title, String originalTitle, Integer year) {
        List<Movie> candidates = movieRepository.findAll();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        String normalizedTitle = TitleNormalizer.normalize(title);
        String normalizedOriginal = TitleNormalizer.normalize(originalTitle);

        if (normalizedTitle.isEmpty()) {
            return Optional.empty();
        }

        Movie bestMatch = null;
        double bestScore = 0.0;

        for (Movie candidate : candidates) {
            String candidateTitle = TitleNormalizer.normalize(candidate.getTitle());
            String candidateOriginal = TitleNormalizer.normalize(candidate.getOriginalTitle());

            // Compute max similarity across all title pair combinations
            double score = maxSimilarity(normalizedTitle, normalizedOriginal,
                    candidateTitle, candidateOriginal);

            if (score < SIMILARITY_THRESHOLD) {
                continue;
            }

            // Year disambiguation: if both have year and differ by more than 1, skip
            if (year != null && candidate.getYear() != null
                    && Math.abs(year - candidate.getYear()) > 1) {
                log.debug("Title similarity for '{}' matches '{}' but year differs ({} vs {}), skipping",
                        title, candidate.getTitle(), year, candidate.getYear());
                continue;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        if (bestMatch != null) {
            log.info("Matched '{}' to existing Movie '{}' (ID: {}), score={}",
                    title, bestMatch.getTitle(), bestMatch.getId(),
                    String.format("%.3f", bestScore));
        } else {
            log.debug("No match found for '{}', will create new Movie", title);
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * Fills null/empty fields on an existing Movie with data from a new import.
     * Never overwrites existing non-null values. Title is intentionally not modified.
     *
     * @return the enriched Movie (saved if any changes were made)
     */
    public Movie enrichMovie(Movie existing, String title, String originalTitle,
                             String posterUrl, Integer durationMinutes,
                             String description, Integer year) {
        boolean changed = false;

        if (isNullOrEmpty(existing.getOriginalTitle()) && isNotEmpty(originalTitle)) {
            existing.setOriginalTitle(originalTitle);
            changed = true;
        }
        if (isNullOrEmpty(existing.getPosterUrl()) && isNotEmpty(posterUrl)) {
            existing.setPosterUrl(posterUrl);
            changed = true;
        }
        if (existing.getDurationMinutes() == null && durationMinutes != null) {
            existing.setDurationMinutes(durationMinutes);
            changed = true;
        }
        if (isNullOrEmpty(existing.getDescription()) && isNotEmpty(description)) {
            existing.setDescription(description);
            changed = true;
        }
        if (existing.getYear() == null && year != null) {
            existing.setYear(year);
            changed = true;
        }

        if (changed) {
            movieRepository.save(existing);
            log.info("Enriched Movie '{}' (ID: {}) with additional data",
                    existing.getTitle(), existing.getId());
        }

        return existing;
    }

    /**
     * Computes the maximum similarity score across all four title pair combinations:
     * input-title vs candidate-title, input-title vs candidate-originalTitle,
     * input-originalTitle vs candidate-title, input-originalTitle vs candidate-originalTitle.
     */
    private double maxSimilarity(String inputTitle, String inputOriginal,
                                 String candidateTitle, String candidateOriginal) {
        double score = computeSimilarity(inputTitle, candidateTitle);

        if (isNotEmpty(candidateOriginal)) {
            score = Math.max(score, computeSimilarity(inputTitle, candidateOriginal));
        }
        if (isNotEmpty(inputOriginal)) {
            score = Math.max(score, computeSimilarity(inputOriginal, candidateTitle));
        }
        if (isNotEmpty(inputOriginal) && isNotEmpty(candidateOriginal)) {
            score = Math.max(score, computeSimilarity(inputOriginal, candidateOriginal));
        }

        return score;
    }

    /**
     * Computes a similarity score between two normalized titles:
     * 1.0 for exact match, 0.85 for prefix match, Levenshtein-based otherwise.
     */
    private double computeSimilarity(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        // Exact match (fast path)
        if (a.equals(b)) {
            return 1.0;
        }

        // Skip if lengths are too different
        double lengthRatio = (double) Math.min(a.length(), b.length())
                / Math.max(a.length(), b.length());
        if (lengthRatio < (1.0 / LENGTH_RATIO_LIMIT)) {
            return 0.0;
        }

        // Prefix match: shorter string is a prefix of longer one
        String shorter = a.length() <= b.length() ? a : b;
        String longer = a.length() > b.length() ? a : b;

        if (longer.startsWith(shorter)) {
            double ratio = (double) shorter.length() / longer.length();
            if (ratio >= PREFIX_LENGTH_RATIO) {
                return PREFIX_MATCH_SCORE;
            }
            // Short prefix — not confident enough, use the ratio as-is
            return ratio;
        }

        // Fuzzy match via Levenshtein distance
        int distance = LevenshteinDistance.compute(a, b);
        return 1.0 - ((double) distance / Math.max(a.length(), b.length()));
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
