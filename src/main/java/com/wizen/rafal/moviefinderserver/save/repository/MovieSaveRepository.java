package com.wizen.rafal.moviefinderserver.save.repository;

import com.wizen.rafal.moviefinderserver.save.model.MovieSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieSaveRepository extends JpaRepository<MovieSave, Long> {

	Optional<MovieSave> findByOriginalId(String originalId);

	boolean existsByOriginalId(String originalId);

	@Query("SELECT COALESCE(MAX(m.id), 0) FROM Movie m")
	Long findMaxId();
}
