package com.wizen.rafal.moviefinderserver.gsc.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.wizen.rafal.moviefinderserver.gsc.config.GscReportProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides short-lived OAuth access tokens for Google APIs using a service
 * account key (read-only webmasters scope). Credentials are resolved lazily
 * on the first call so a missing or invalid key never breaks app startup.
 */
@Component
@RequiredArgsConstructor
public class GoogleAccessTokenProvider {

	private static final String SCOPE = "https://www.googleapis.com/auth/webmasters.readonly";

	private final GscReportProperties properties;

	private GoogleCredentials credentials;

	public synchronized String getAccessToken() throws IOException {
		if (credentials == null) {
			credentials = GoogleCredentials
					.fromStream(new ByteArrayInputStream(resolveCredentialsJson().getBytes(StandardCharsets.UTF_8)))
					.createScoped(SCOPE);
		}
		credentials.refreshIfExpired();
		return credentials.getAccessToken().getTokenValue();
	}

	private String resolveCredentialsJson() throws IOException {
		String inline = properties.getCredentialsJson();
		String file = properties.getCredentialsFile();
		boolean hasInline = inline != null && !inline.isBlank();
		boolean hasFile = file != null && !file.isBlank();
		if (hasInline == hasFile) {
			throw new IllegalStateException("Set exactly one of gsc.report.credentials-json or gsc.report.credentials-file");
		}
		return hasFile ? Files.readString(Path.of(file)) : inline;
	}
}
