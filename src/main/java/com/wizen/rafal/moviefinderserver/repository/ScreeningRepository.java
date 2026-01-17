package com.wizen.rafal.moviefinderserver.repository;

import com.wizen.rafal.moviefinderserver.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

	@Query("SELECT s FROM Screening s " +
			"JOIN FETCH s.movie m " +
			"JOIN FETCH s.cinema c " +
			"WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')) " +
			"ORDER BY s.screeningDatetime")
	List<Screening> findScreeningsByMovieTitle(@Param("title") String title);
}