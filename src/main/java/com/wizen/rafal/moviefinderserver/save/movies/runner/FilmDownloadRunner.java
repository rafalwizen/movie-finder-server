package com.wizen.rafal.moviefinderserver.save.movies.runner;

import com.wizen.rafal.moviefinderserver.save.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("download-films")
@RequiredArgsConstructor
public class FilmDownloadRunner implements CommandLineRunner {

    private static final String PROVIDER_PROFILE_PREFIX = "download-films-";

    private final ImportService importService;
    private final Environment environment;

    @Override
    public void run(String... args) {
        importService.importFilms(resolveSpecificProvider());
    }

    private String resolveSpecificProvider() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.startsWith(PROVIDER_PROFILE_PREFIX)) {
                return profile.substring(PROVIDER_PROFILE_PREFIX.length());
            }
        }
        return null;
    }
}
