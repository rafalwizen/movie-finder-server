package com.wizen.rafal.moviefinderserver.save.movies.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "movies")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSave {

	@Id
	private Long id;

	@Column(name = "title")
	private String title;

	@Column(name = "original_title")
	private String originalTitle;

	@Column(name = "year")
	private Integer year;

	@Column(name = "duration_minutes")
	private Integer durationMinutes;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "poster_url", columnDefinition = "TEXT")
	private String posterUrl;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}