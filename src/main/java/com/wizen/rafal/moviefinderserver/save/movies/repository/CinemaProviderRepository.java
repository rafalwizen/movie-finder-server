package com.wizen.rafal.moviefinderserver.save.movies.repository;

import com.wizen.rafal.moviefinderserver.save.movies.model.CinemaProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CinemaProviderRepository extends JpaRepository<CinemaProvider, Long> {

	Optional<CinemaProvider> findByCode(String code);
}
