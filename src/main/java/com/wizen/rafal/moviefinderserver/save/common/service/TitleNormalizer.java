package com.wizen.rafal.moviefinderserver.save.common.service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Normalizes movie titles for cross-provider fuzzy matching.
 * Strips diacritics, cinema-specific suffixes, and whitespace noise.
 */
public final class TitleNormalizer {

    private TitleNormalizer() {
    }

    // Suffixes commonly appended by cinema providers in Poland
    private static final List<Pattern> PROVIDER_SUFFIXES = List.of(
            Pattern.compile("\\s*-\\s*koncert.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*-\\s*spektakl.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*-\\s* transmisja.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*3d$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*imax$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*vip$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*napisy.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*dubbing.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\s*lektor.*$", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Normalizes a title for comparison: lowercase, strip diacritics,
     * remove content in parentheses, strip provider-specific suffixes,
     * and collapse whitespace.
     */
    public static String normalize(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String result = title.trim().toLowerCase();

        // Remove diacritics (e.g., ł -> l, ą -> a, ę -> e)
        result = Normalizer.normalize(result, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // Remove content in parentheses: "(Napisy)", "(Dubbing)", etc.
        result = result.replaceAll("\\s*\\([^)]*\\)", "");

        // Remove provider-specific suffixes
        for (Pattern suffix : PROVIDER_SUFFIXES) {
            result = suffix.matcher(result).replaceAll("").trim();
        }

        // Collapse multiple whitespace into single space
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }
}
