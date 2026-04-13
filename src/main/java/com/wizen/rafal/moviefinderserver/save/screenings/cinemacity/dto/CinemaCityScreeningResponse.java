package com.wizen.rafal.moviefinderserver.save.screenings.cinemacity.dto;

import lombok.Data;

import java.util.List;

@Data
public class CinemaCityScreeningResponse {

    private Body body;

    @Data
    public static class Body {
        private List<Event> events;
    }

    @Data
    public static class Event {
        private String filmId;
        private String cinemaId;
        private String businessDay;
        private String eventDateTime;
        private String bookingLink;
    }
}
