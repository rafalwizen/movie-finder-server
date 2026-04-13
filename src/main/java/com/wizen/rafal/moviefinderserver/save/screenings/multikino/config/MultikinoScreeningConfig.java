package com.wizen.rafal.moviefinderserver.save.screenings.multikino.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.multikino.screenings")
@Getter
@Setter
public class MultikinoScreeningConfig {

    private String baseUrl;
    private int rateLimitMs = 500;
    private boolean enabled = true;
}
