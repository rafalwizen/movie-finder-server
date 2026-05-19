package com.wizen.rafal.moviefinderserver.save.cinemas.helios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.helios.cinemas")
@Getter
@Setter
public class HeliosCinemaConfig {

    private String url = "https://helios.pl/";
    private boolean enabled = true;
    private boolean fetchDetails = true;
    private int rateLimitMs = 1000;
}
