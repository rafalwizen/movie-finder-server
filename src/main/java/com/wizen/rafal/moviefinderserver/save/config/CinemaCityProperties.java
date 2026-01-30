package com.wizen.rafal.moviefinderserver.save.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "cinema-city.api")
public class CinemaCityProperties {

	private String baseUrl;
	private List<String> cinemaIds;

}
