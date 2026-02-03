package com.wizen.rafal.moviefinderserver.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
@Data
public class Screening {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", columnDefinition = "INTEGER")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "movie_id")
	private Movie movie;

	@ManyToOne
	@JoinColumn(name = "cinema_id", columnDefinition = "INTEGER")
	private Cinema cinema;

	@Column(name = "screening_datetime")
	private LocalDateTime screeningDatetime;

	@Column(name = "screening_url")
	private String screeningUrl;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}