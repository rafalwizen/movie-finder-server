package com.wizen.rafal.moviefinderserver.gsc.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wizen.rafal.moviefinderserver.gsc.config.TelegramProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Sends HTML-formatted messages through the Telegram Bot API, splitting
 * anything longer than the 4096-character message limit into chunks
 * (split points are line breaks so HTML tags never get cut in half).
 */
@Component
@RequiredArgsConstructor
public class TelegramNotifier {

	private static final int SAFE_LIMIT = 4000;

	private final RestTemplate restTemplate;
	private final TelegramProperties properties;

	public void sendHtml(String html) {
		for (String chunk : split(html)) {
			sendChunk(chunk);
		}
	}

	private void sendChunk(String chunk) {
		String url = "https://api.telegram.org/bot" + properties.getBotToken() + "/sendMessage";
		SendMessageRequest request = new SendMessageRequest(properties.getChatId(), chunk, "HTML", true);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		try {
			restTemplate.postForObject(URI.create(url), new HttpEntity<>(request, headers), String.class);
		} catch (RestClientResponseException e) {
			throw new IllegalStateException("Telegram send failed: HTTP " + e.getStatusCode().value() + " " + e.getResponseBodyAsString(), e);
		}
	}

	private static List<String> split(String text) {
		List<String> chunks = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String line : text.split("\n", -1)) {
			if (line.length() > SAFE_LIMIT) {
				// Pathological single line: hard-split (never expected with the report template)
				for (int i = 0; i < line.length(); i += SAFE_LIMIT) {
					appendLine(chunks, current, line.substring(i, Math.min(line.length(), i + SAFE_LIMIT)));
				}
			} else {
				appendLine(chunks, current, line);
			}
		}
		if (!current.isEmpty()) {
			chunks.add(current.toString());
		}
		return chunks;
	}

	private static void appendLine(List<String> chunks, StringBuilder current, String line) {
		if (!current.isEmpty() && current.length() + 1 + line.length() > SAFE_LIMIT) {
			chunks.add(current.toString());
			current.setLength(0);
		}
		if (!current.isEmpty()) {
			current.append('\n');
		}
		current.append(line);
	}

	private record SendMessageRequest(
			@JsonProperty("chat_id") String chatId,
			String text,
			@JsonProperty("parse_mode") String parseMode,
			@JsonProperty("disable_web_page_preview") boolean disableWebPagePreview) {
	}
}
