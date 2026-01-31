package com.wizen.rafal.moviefinderserver.save.movies.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CinemaCityResponse {

	private List<FilmDto> body;

	@Data
	public static class FilmDto {

		@JsonProperty("filmId")
		private String filmId;

		@JsonProperty("filmName")
		private String filmName;

		@JsonProperty("filmLink")
		private String filmLink;

		@JsonProperty("videoLink")
		private String videoLink;
	}
}
