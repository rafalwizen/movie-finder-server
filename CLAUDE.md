# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./mvnw clean package                    # Build JAR
./mvnw clean package -DskipTests        # Build without tests
./mvnw test                             # Run tests
./mvnw spring-boot:run                  # Run API server (default profile, port 8080)
```

### Data Ingestion Profiles

The app uses Spring profiles to trigger one-shot data imports from the Cinema City API:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=fetch-cinemas       # Fetch cinema locations
./mvnw spring-boot:run -Dspring-boot.run.profiles=download-films      # Download film metadata
./mvnw spring-boot:run -Dspring-boot.run.profiles=fetch-screenings    # Fetch screening showtimes
```

### Docker

```bash
docker-compose up --build               # Build and run containerized on port 8080
```

## Architecture

**Stack:** Java 21, Spring Boot 3.4.1, Spring Data JPA, SQLite, Lombok, Maven

Two operational modes:

1. **REST API server** (no active profile) — serves movie/screening search endpoints from the SQLite DB. Controllers (`search/controller/`) query Spring Data JPA repositories directly; there is no separate service layer for reads.

2. **Data ingestion** (profile-activated `CommandLineRunner` beans) — each profile triggers a one-shot import process that calls the Cinema City public API and persists results to SQLite. Each ingestion module follows the same structure: `config/` → `dto/` → `runner/` → `service/`.

### Domain Model (5 entities)

- `CinemaProvider` — cinema chain (e.g., "CINEMA_CITY")
- `Cinema` — physical location with lat/long, linked to a provider
- `Movie` — movie metadata (title, year, duration, poster URL)
- `MovieSource` — maps external provider IDs to internal movie IDs
- `Screening` — individual showtime, linked to a movie and cinema

### Package Structure

- `domain/model/` — JPA entities
- `domain/repository/` — Spring Data JPA repositories
- `save/cinemas/` — cinema data ingestion from Cinema City
- `save/movies/` — film data ingestion from Cinema City
- `save/screenings/` — screening data ingestion from Cinema City
- `search/controller/` — REST controllers (`MovieController`, `ScreeningController`)
- `search/dto/` — response DTOs

### Database

SQLite at `db/cinema.db`. Hibernate with `ddl-auto=update` manages the schema. Manual schema reference in `db_create.sql`. No migration tool (Flyway/Liquibase).

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/movies` | Movies with active screenings; optional `?q=` title search |
| GET | `/api/movies/allMovies` | All movies regardless of screenings |
| GET | `/api/screenings/by-movie` | Screenings by `?movieId=`; optional `&includePast=true` |

Full API docs in `API.md`.
