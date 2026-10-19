package com.wizen.rafal.moviefinderserver.gsc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
@Getter
@Setter
public class TelegramProperties {

	private String botToken = "";
	private String chatId = "";
}
