package com.wizen.rafal.moviefinderserver.save.cinemas.cinemacity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.cinema-city.cinemas")
@Getter
@Setter
public class CinemaCityCinemaConfig {

    private String url;
    private boolean enabled = true;
}
