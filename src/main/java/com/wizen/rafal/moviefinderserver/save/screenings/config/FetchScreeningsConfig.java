package com.wizen.rafal.moviefinderserver.save.screenings.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile("fetch-screenings")
public class FetchScreeningsConfig {

	@Bean
	public RestTemplate screeningsRestTemplate() {
		return new RestTemplate();
	}
}
