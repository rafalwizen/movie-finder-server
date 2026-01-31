package com.wizen.rafal.moviefinderserver.save.screenings.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_sources")
@Data
public class MovieSourceScreening {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "movie_id", nullable = false)
	private Long movieId;

	@Column(name = "provider_id", nullable = false)
	private Long providerId;

	@Column(name = "external_movie_id", nullable = false)
	private String externalMovieId;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}
