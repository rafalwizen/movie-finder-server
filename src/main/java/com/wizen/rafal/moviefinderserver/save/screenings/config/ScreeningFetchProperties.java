package com.wizen.rafal.moviefinderserver.save.screenings.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "screening-fetch")
@Getter
@Setter
public class ScreeningFetchProperties {

	private int daysAhead = 0;
	private Cleanup cleanup = new Cleanup();

	@Getter
	@Setter
	public static class Cleanup {
		private boolean enabled = false;
		private Integer daysToKeep = null;
	}
}
