package com.wizen.rafal.moviefinderserver.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
public class CinemaId implements Serializable {

	private Long id;
	private Long providerId;

	public CinemaId() {
	}

	public CinemaId(Long id, Long providerId) {
		this.id = id;
		this.providerId = providerId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CinemaId cinemaId = (CinemaId) o;
		return Objects.equals(id, cinemaId.id) && Objects.equals(providerId, cinemaId.providerId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, providerId);
	}
}
