package com.wizen.rafal.moviefinderserver.save.movies.multikino.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultikinoFilmResponse {

    private List<FilmDto> result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FilmDto {
        private String filmId;
        private String filmTitle;
        private String originalTitle;
        private String posterImageSrc;
        private String synopsisShort;
        private Integer runningTime;
        private String releaseDate;
        private String director;
        private List<String> genres;
        private List<String> showingInCinemas;
    }
}
