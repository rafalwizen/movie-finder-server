package com.wizen.rafal.moviefinderserver.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cinemas")
@Getter
@Setter
public class Cinema {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", columnDefinition = "INTEGER")
	private Long id;

	@Column(name = "provider_id", nullable = false)
	private Long providerId;

	@Column(name = "external_cinema_id", nullable = false)
	private String externalCinemaId;

	@Column(name = "name")
	private String name;

	@Column(name = "city")
	private String city;

	@Column(name = "address")
	private String address;

	@Column(name = "website_url")
	private String websiteUrl;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}