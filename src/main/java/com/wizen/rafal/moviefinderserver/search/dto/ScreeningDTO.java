package com.wizen.rafal.moviefinderserver.search.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ScreeningDTO {
	private LocalDateTime screeningDatetime;
	private String cinemaName;
	private String cinemaCity;
	private String cinemaAddress;
	private String screeningUrl;
	private String providerCode;

	public ScreeningDTO(LocalDateTime screeningDatetime, String cinemaName,
						String cinemaCity, String cinemaAddress, String screeningUrl,
						String providerCode) {
		this.screeningDatetime = screeningDatetime;
		this.cinemaName = cinemaName;
		this.cinemaCity = cinemaCity;
		this.cinemaAddress = cinemaAddress;
		this.screeningUrl = screeningUrl;
		this.providerCode = providerCode;
	}
}