package com.wizen.rafal.moviefinderserver.save.controller;

import com.wizen.rafal.moviefinderserver.save.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/cinemas")
    public ResponseEntity<Map<String, String>> importCinemas() {
        CompletableFuture.runAsync(() -> importService.importCinemas());
        return ResponseEntity.accepted().body(Map.of("status", "cinema import started"));
    }

    @PostMapping("/films")
    public ResponseEntity<Map<String, String>> importFilms() {
        CompletableFuture.runAsync(() -> importService.importFilms());
        return ResponseEntity.accepted().body(Map.of("status", "film import started"));
    }

    @PostMapping("/screenings")
    public ResponseEntity<Map<String, String>> importScreenings() {
        CompletableFuture.runAsync(() -> importService.importScreenings());
        return ResponseEntity.accepted().body(Map.of("status", "screening import started"));
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, String>> importAll() {
        CompletableFuture.runAsync(() -> importService.importAll());
        return ResponseEntity.accepted().body(Map.of("status", "full import started"));
    }
}
