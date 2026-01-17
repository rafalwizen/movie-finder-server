package com.wizen.rafal.moviefinderserver.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
@Data
public class Screening {
	@Id
	private Long id;

	@ManyToOne
	@JoinColumn(name = "movie_id")
	private Movie movie;

	@ManyToOne
	@JoinColumn(name = "cinema_id")
	private Cinema cinema;

	@Column(name = "screening_datetime")
	private LocalDateTime screeningDatetime;

	@Column(name = "screening_url")
	private String screeningUrl;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}