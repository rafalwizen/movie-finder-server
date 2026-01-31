package com.wizen.rafal.moviefinderserver.save.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "movie_sources", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"provider_id", "external_movie_id"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSource {

	@Id
	private Long id;

	@Column(name = "movie_id", nullable = false)
	private Long movieId;

	@Column(name = "provider_id", nullable = false)
	private Long providerId;

	@Column(name = "external_movie_id", nullable = false)
	private String externalMovieId;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "movie_id", insertable = false, updatable = false)
	private MovieSave movie;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "provider_id", insertable = false, updatable = false)
	private CinemaProvider provider;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
