package com.wizen.rafal.moviefinderserver.domain.repository;

import com.wizen.rafal.moviefinderserver.domain.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

	@Query("SELECT DISTINCT m FROM Movie m " +
			"JOIN Screening s ON s.movie.id = m.id " +
			"WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
			"ORDER BY m.title")
	List<Movie> findMoviesWithActiveScreenings(@Param("query") String query);

	@Query("SELECT DISTINCT m FROM Movie m " +
			"JOIN Screening s ON s.movie.id = m.id " +
			"ORDER BY m.title")
	List<Movie> findAllMoviesWithActiveScreenings();


	Optional<Movie> findById(Long Id);
}