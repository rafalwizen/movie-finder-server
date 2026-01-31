package com.wizen.rafal.moviefinderserver.save.repository;

import com.wizen.rafal.moviefinderserver.save.model.MovieSave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieSaveRepository extends JpaRepository<MovieSave, Long> {

	@Query("SELECT COALESCE(MAX(m.id), 0) FROM Movie m")
	Long findMaxId();
}
