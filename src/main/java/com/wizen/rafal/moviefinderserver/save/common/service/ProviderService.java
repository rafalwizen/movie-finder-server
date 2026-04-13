package com.wizen.rafal.moviefinderserver.save.common.service;

import com.wizen.rafal.moviefinderserver.domain.model.CinemaProvider;
import com.wizen.rafal.moviefinderserver.domain.repository.CinemaProviderRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieRepository;
import com.wizen.rafal.moviefinderserver.domain.repository.MovieSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderService {

    private final CinemaProviderRepository cinemaProviderRepository;
    private final MovieRepository movieRepository;
    private final MovieSourceRepository movieSourceRepository;

    public CinemaProvider getOrCreateProvider(String code, String name) {
        Optional<CinemaProvider> existing = cinemaProviderRepository.findByCode(code);

        if (existing.isPresent()) {
            log.debug("Provider {} already exists", code);
            return existing.get();
        }

        Long newId = cinemaProviderRepository.findAll().stream()
                .map(CinemaProvider::getId)
                .max(Long::compareTo)
                .orElse(0L) + 1;

        CinemaProvider provider = CinemaProvider.builder()
                .id(newId)
                .code(code)
                .name(name)
                .build();

        cinemaProviderRepository.save(provider);
        log.info("Created new provider: {} (ID: {})", name, newId);

        return provider;
    }

    public Long nextMovieId() {
        return movieRepository.findMaxId() + 1;
    }

    public Long nextMovieSourceId() {
        return movieSourceRepository.findMaxId() + 1;
    }
}
