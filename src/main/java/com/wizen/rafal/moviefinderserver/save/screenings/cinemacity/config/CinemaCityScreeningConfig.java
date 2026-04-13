package com.wizen.rafal.moviefinderserver.save.screenings.cinemacity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.cinema-city.screenings")
@Getter
@Setter
public class CinemaCityScreeningConfig {

    private String urlTemplate;
    private boolean enabled = true;
}
