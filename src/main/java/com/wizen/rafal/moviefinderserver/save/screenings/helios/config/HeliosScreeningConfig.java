package com.wizen.rafal.moviefinderserver.save.screenings.helios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.helios.screenings")
@Getter
@Setter
public class HeliosScreeningConfig {

    private String baseUrl = "https://helios.pl";
    private boolean enabled = true;
    private int rateLimitMs = 1000;
    private int cinemaLimit = 0;
}
