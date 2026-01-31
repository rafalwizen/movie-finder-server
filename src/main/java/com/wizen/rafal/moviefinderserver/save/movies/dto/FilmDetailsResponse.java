package com.wizen.rafal.moviefinderserver.save.movies.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FilmDetailsResponse {

	private Body body;

	@Data
	public static class Body {

		@JsonProperty("filmDetails")
		private FilmDetails filmDetails;
	}

	@Data
	public static class FilmDetails {

		@JsonProperty("id")
		private String id;

		@JsonProperty("name")
		private String name;

		@JsonProperty("originalName")
		private String originalName;

		@JsonProperty("length")
		private Integer length;

		@JsonProperty("posterLink")
		private String posterLink;
	}
}