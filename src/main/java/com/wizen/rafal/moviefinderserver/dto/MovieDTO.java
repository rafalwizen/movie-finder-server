package com.wizen.rafal.moviefinderserver.dto;

import lombok.Data;

@Data
public class MovieDTO {
	private Long id;
	private String title;
	private Integer year;
	private String posterUrl;

	public MovieDTO(Long id, String title, Integer year, String posterUrl) {
		this.id = id;
		this.title = title;
		this.year = year;
		this.posterUrl = posterUrl;
	}
}