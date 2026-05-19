package com.wizen.rafal.moviefinderserver.save.movies.helios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.helios.films")
@Getter
@Setter
public class HeliosFilmConfig {

    private String baseUrl = "https://helios.pl";
    private boolean enabled = true;
    private boolean fetchFilmDetails = true;
    private int rateLimitMs = 1000;
    private int cinemaLimit = 0;
}
