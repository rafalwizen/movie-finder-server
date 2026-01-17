package com.wizen.rafal.moviefinderserver.controller;

import com.wizen.rafal.moviefinderserver.dto.ScreeningDTO;
import com.wizen.rafal.moviefinderserver.model.Screening;
import com.wizen.rafal.moviefinderserver.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/screenings")
@RequiredArgsConstructor
public class ScreeningController {

	private final ScreeningRepository screeningRepository;

	@GetMapping("/search")
	public List<ScreeningDTO> searchScreenings(@RequestParam String title) {
		List<Screening> screenings = screeningRepository.findScreeningsByMovieTitle(title);

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