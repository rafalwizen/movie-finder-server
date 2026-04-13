package com.wizen.rafal.moviefinderserver.save.screenings.multikino.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultikinoScreeningResponse {

    private List<ShowingResult> result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShowingResult {
        private List<ShowingGroup> showingGroups;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShowingGroup {
        private String date;
        private List<Session> sessions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Session {
        private String sessionId;
        private String startTime;
        private String endTime;
        private Integer duration;
        private String screenName;
        private String bookingUrl;
    }
}
