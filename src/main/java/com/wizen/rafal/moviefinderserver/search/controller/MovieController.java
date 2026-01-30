package com.wizen.rafal.moviefinderserver.search.controller;

import com.wizen.rafal.moviefinderserver.search.dto.MovieDTO;
import com.wizen.rafal.moviefinderserver.search.model.Movie;
import com.wizen.rafal.moviefinderserver.search.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

	private final MovieRepository movieRepository;

	@GetMapping
	public List<MovieDTO> getMovies(@RequestParam(required = false) String q) {
		List<Movie> movies;

		if (q != null && !q.trim().isEmpty()) {
			movies = movieRepository.findMoviesWithActiveScreenings(q);
		} else {
			movies = movieRepository.findAllMoviesWithActiveScreenings();
		}

		return movies.stream()
				.map(m -> new MovieDTO(m.getId(), m.getTitle(), m.getYear(), m.getPosterUrl()))
				.collect(Collectors.toList());
	}
}