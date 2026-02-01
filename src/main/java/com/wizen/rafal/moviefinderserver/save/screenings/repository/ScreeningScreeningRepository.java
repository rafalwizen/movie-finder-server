package com.wizen.rafal.moviefinderserver.save.screenings.repository;

import com.wizen.rafal.moviefinderserver.save.screenings.model.ScreeningScreening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ScreeningScreeningRepository extends JpaRepository<ScreeningScreening, Long> {

	boolean existsByMovieIdAndCinemaIdAndScreeningDatetime(
			Long movieId,
			Long cinemaId,
			LocalDateTime screeningDatetime
	);
}
