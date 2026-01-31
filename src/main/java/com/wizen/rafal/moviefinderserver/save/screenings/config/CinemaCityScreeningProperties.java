package com.wizen.rafal.moviefinderserver.save.screenings.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cinema-city-screenings.api")
@Getter
@Setter
public class CinemaCityScreeningProperties {

	private String baseUrl;
	private String filmDetailsUrl;
	private String cinemaIds;
}
