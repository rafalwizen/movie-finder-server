package com.wizen.rafal.moviefinderserver.save.screenings.repository;

import com.wizen.rafal.moviefinderserver.save.screenings.model.MovieSourceScreening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieSourceScreeningRepository extends JpaRepository<MovieSourceScreening, Long> {

	List<MovieSourceScreening> findByProviderId(Long providerId);
}
