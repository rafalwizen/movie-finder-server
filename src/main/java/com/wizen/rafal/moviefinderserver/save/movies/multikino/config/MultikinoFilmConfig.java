package com.wizen.rafal.moviefinderserver.save.movies.multikino.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "providers.multikino.films")
@Getter
@Setter
public class MultikinoFilmConfig {

    private String url;
    private boolean enabled = true;
}
