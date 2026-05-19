package com.wizen.rafal.moviefinderserver.save.common.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HeliosPageFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 15000;

    public Document fetchPage(String url, int rateLimitMs) {
        rateLimit(rateLimitMs);
        log.debug("Fetching Helios page: {}", url);
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Helios page: " + url, e);
        }
    }

    private void rateLimit(int ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
