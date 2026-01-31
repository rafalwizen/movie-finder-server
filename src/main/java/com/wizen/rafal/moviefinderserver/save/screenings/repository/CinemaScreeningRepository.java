package com.wizen.rafal.moviefinderserver.save.screenings.repository;

import com.wizen.rafal.moviefinderserver.save.screenings.model.CinemaScreening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CinemaScreeningRepository extends JpaRepository<CinemaScreening, Long> {

	Optional<CinemaScreening> findByIdAndProviderId(Long id, Long providerId);
}
