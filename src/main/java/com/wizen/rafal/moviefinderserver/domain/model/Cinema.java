package com.wizen.rafal.moviefinderserver.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "cinemas")
@Data
public class Cinema {
	@Id
	private Long id;

	private String name;

	private String city;

	private String address;

	@Column(name = "website_url")
	private String websiteUrl;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}