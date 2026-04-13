package com.wizen.rafal.moviefinderserver.save.movies.cinemacity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "providers.cinema-city.films")
public class CinemaCityFilmConfig {

    private String baseUrl;
    private String filmDetailsUrl;
    private List<String> cinemaIds;
    private boolean enabled = true;
}
