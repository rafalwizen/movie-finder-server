package com.wizen.rafal.moviefinderserver.domain.model;

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
public class Movie {

	@Id
	private Long id;

	private String title;

	@Column(name = "original_title")
	private String originalTitle;

	private Integer year;

	@Column(name = "duration_minutes")
	private Integer durationMinutes;

	private String description;

	@Column(name = "poster_url")
	private String posterUrl;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}