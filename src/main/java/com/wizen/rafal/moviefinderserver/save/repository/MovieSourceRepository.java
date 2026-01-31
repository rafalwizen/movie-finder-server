package com.wizen.rafal.moviefinderserver.save.repository;

import com.wizen.rafal.moviefinderserver.save.model.MovieSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieSourceRepository extends JpaRepository<MovieSource, Long> {

	Optional<MovieSource> findByProviderIdAndExternalMovieId(Long providerId, String externalMovieId);

	boolean existsByProviderIdAndExternalMovieId(Long providerId, String externalMovieId);

	@Query("SELECT COALESCE(MAX(ms.id), 0) FROM MovieSource ms")
	Long findMaxId();
}
