package com.wizen.rafal.moviefinderserver.save.cinemas.multikino.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultikinoCinemaResponse {

    private List<AlphaGroup> result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlphaGroup {
        private String alpha;
        private List<CinemaDto> cinemas;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CinemaDto {
        private String cinemaId;
        private String cinemaName;
        private String fullName;
        private String whatsOnUrl;
    }
}
