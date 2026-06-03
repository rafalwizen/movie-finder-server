package com.wizen.rafal.moviefinderserver.save.common.service;

/**
 * Computes the Levenshtein distance between two strings
 * using a standard dynamic-programming approach with 2-row memory optimization.
 */
public final class LevenshteinDistance {

    private LevenshteinDistance() {
    }

    /**
     * Returns the minimum number of single-character edits
     * (insertions, deletions, or substitutions) required to change
     * one string into the other.
     */
    public static int compute(String a, String b) {
        if (a == null || b == null) {
            return Integer.MAX_VALUE;
        }
        if (a.isEmpty()) {
            return b.length();
        }
        if (b.isEmpty()) {
            return a.length();
        }

        // Ensure 'a' is the shorter string to minimize row length
        if (a.length() > b.length()) {
            String tmp = a;
            a = b;
            b = tmp;
        }

        int[] prevRow = new int[a.length() + 1];
        int[] currRow = new int[a.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            prevRow[i] = i;
        }

        for (int j = 1; j <= b.length(); j++) {
            currRow[0] = j;
            for (int i = 1; i <= a.length(); i++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                currRow[i] = Math.min(
                        Math.min(currRow[i - 1] + 1, prevRow[i] + 1),
                        prevRow[i - 1] + cost
                );
            }
            int[] swap = prevRow;
            prevRow = currRow;
            currRow = swap;
        }

        return prevRow[a.length()];
    }
}
