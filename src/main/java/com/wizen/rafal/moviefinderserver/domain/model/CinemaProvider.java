package com.wizen.rafal.moviefinderserver.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "cinema_providers")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaProvider {

	@Id
	private Long id;

	@Column(name = "code", nullable = false, unique = true, length = 50)
	private String code;

	@Column(name = "name", nullable = false)
	private String name;
}
