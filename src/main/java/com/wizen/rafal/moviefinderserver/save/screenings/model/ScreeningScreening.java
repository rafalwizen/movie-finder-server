package com.wizen.rafal.moviefinderserver.save.screenings.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
@Data
public class ScreeningScreening {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "movie_id", nullable = false)
	private Long movieId;

	@Column(name = "cinema_id", nullable = false)
	private Long cinemaId;

	@Column(name = "screening_datetime")
	private LocalDateTime screeningDatetime;

	@Column(name = "screening_url", columnDefinition = "TEXT")
	private String screeningUrl;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}
