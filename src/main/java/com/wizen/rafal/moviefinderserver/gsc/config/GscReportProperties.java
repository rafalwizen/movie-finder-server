package com.wizen.rafal.moviefinderserver.gsc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "gsc.report")
@Getter
@Setter
public class GscReportProperties {

	private String siteUrl = "https://gdzie-do-kina.pl/";
	private List<String> monitoredUrls = new ArrayList<>(List.of("https://gdzie-do-kina.pl/"));
	private int days = 7;
	private int dataLagDays = 3;
	private String zone = "Europe/Warsaw";
	private String credentialsJson = "";
	private String credentialsFile = "";
}
