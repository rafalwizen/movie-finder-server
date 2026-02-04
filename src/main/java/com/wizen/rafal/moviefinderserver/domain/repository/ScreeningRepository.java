package com.wizen.rafal.moviefinderserver.domain.repository;

import com.wizen.rafal.moviefinderserver.domain.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

	@Query("SELECT s FROM Screening s " +
			"JOIN FETCH s.movie m " +
			"JOIN FETCH s.cinema c " +
			"WHERE m.id = :movieId " +
			"ORDER BY s.screeningDatetime")
	List<Screening> findScreeningsByMovieId(@Param("movieId") Long movieId);

	@Query("SELECT s FROM Screening s " +
			"JOIN FETCH s.movie m " +
			"JOIN FETCH s.cinema c " +
			"WHERE m.id = :movieId " +
			"AND s.screeningDatetime >= CURRENT_TIMESTAMP " +
			"ORDER BY s.screeningDatetime")
	List<Screening> findFutureScreeningsByMovieId(@Param("movieId") Long movieId);

	@Modifying
	@Query("DELETE FROM Screening s WHERE s.screeningDatetime < :cutoffDateTime")
	int deleteByScreeningDatetimeBefore(@Param("cutoffDateTime") LocalDateTime cutoffDateTime);

	boolean existsByMovieIdAndCinemaIdAndScreeningDatetime(
			Long movieId,
			Long cinemaId,
			LocalDateTime screeningDatetime
	);
}