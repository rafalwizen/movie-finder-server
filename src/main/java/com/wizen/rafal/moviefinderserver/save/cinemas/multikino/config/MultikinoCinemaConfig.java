package com.wizen.rafal.moviefinderserver.save.cinemas.multikino.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.multikino.cinemas")
@Getter
@Setter
public class MultikinoCinemaConfig {

    private String url;
    private boolean enabled = true;
}
