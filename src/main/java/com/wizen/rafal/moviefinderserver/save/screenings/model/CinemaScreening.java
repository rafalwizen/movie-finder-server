package com.wizen.rafal.moviefinderserver.save.screenings.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "cinemas")
@Data
public class CinemaScreening {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "provider_id", nullable = false)
	private Long providerId;

	@Column(name = "name")
	private String name;

	@Column(name = "city")
	private String city;

	@Column(name = "address")
	private String address;

	@Column(name = "website_url", columnDefinition = "TEXT")
	private String websiteUrl;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}
