package com.wizen.rafal.moviefinderserver.search.dto;

import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import lombok.Data;

@Data
public class MovieDTO {
	private Long id;
	private String title;
	private String originalTitle;
	private Integer year;
	private Integer durationMinutes;
	private String description;
	private String posterUrl;

	public static MovieDTO from(Movie movie) {
		MovieDTO dto = new MovieDTO();
		dto.setId(movie.getId());
		dto.setTitle(movie.getTitle());
		dto.setOriginalTitle(movie.getOriginalTitle());
		dto.setYear(movie.getYear());
		dto.setDurationMinutes(movie.getDurationMinutes());
		dto.setDescription(movie.getDescription());
		dto.setPosterUrl(movie.getPosterUrl());
		return dto;
	}
}
