package com.wizen.rafal.moviefinderserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MovieFinderServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieFinderServerApplication.class, args);
	}

}
