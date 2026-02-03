package com.wizen.rafal.moviefinderserver.domain.repository;

import com.wizen.rafal.moviefinderserver.domain.model.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long> {

	boolean existsByProviderIdAndExternalCinemaId(Long providerId, String externalCinemaId);

	Optional<Cinema> findByProviderIdAndExternalCinemaId(Long providerId, String externalCinemaId);
}
