package com.wizen.rafal.moviefinderserver.search.controller;

import com.wizen.rafal.moviefinderserver.search.dto.ScreeningDTO;
import com.wizen.rafal.moviefinderserver.domain.model.Screening;
import com.wizen.rafal.moviefinderserver.domain.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/screenings")
@RequiredArgsConstructor
public class ScreeningController {

	private final ScreeningRepository screeningRepository;

	@GetMapping("/by-movie")
	public List<ScreeningDTO> getScreeningsByMovie(
			@RequestParam Long movieId,
			@RequestParam(required = false, defaultValue = "false") boolean includePast) {

		List<Screening> screenings;

		if (includePast) {
			screenings = screeningRepository.findScreeningsByMovieId(movieId);
		} else {
			screenings = screeningRepository.findFutureScreeningsByMovieId(movieId);
		}

		return screenings.stream()
				.map(s -> new ScreeningDTO(
						s.getScreeningDatetime(),
						s.getCinema().getName(),
						s.getCinema().getCity(),
						s.getCinema().getAddress(),
						s.getScreeningUrl()
				))
				.collect(Collectors.toList());
	}
}