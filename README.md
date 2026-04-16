# Movie Finder Server

REST API server that aggregates cinema showtimes from multiple providers (Cinema City, Multikino) and exposes them through a unified search interface.

## Tech Stack

- Java 21, Spring Boot 3.4, Spring Data JPA
- SQLite
- Maven
- Docker

## Quick Start

```bash
# Build
./mvnw clean package -DskipTests

# Run API server
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

### Docker

```bash
docker-compose up --build
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/movies` | Movies with active screenings (optional `?q=` title search) |
| `GET` | `/api/allMovies` | All movies regardless of screenings |
| `GET` | `/api/screenings/by-movie?movieId=` | Screenings for a movie (optional `&includePast=true`) |

### Example

```bash
# Search movies
curl "http://localhost:8080/api/movies?q=batman"

# Get screenings
curl "http://localhost:8080/api/screenings/by-movie?movieId=1"
```

Full API documentation: [API.md](API.md)

## Data Import

Cinema data is imported from public APIs using Spring profiles:

```bash
# Fetch cinema locations
./mvnw spring-boot:run -Dspring-boot.run.profiles=fetch-cinemas

# Download film metadata
./mvnw spring-boot:run -Dspring-boot.run.profiles=download-films

# Fetch screening showtimes
./mvnw spring-boot:run -Dspring-boot.run.profiles=fetch-screenings
```

Each profile triggers a one-shot import that persists data to SQLite.

## Project Structure

```
src/main/java/.../moviefinderserver/
├── domain/
│   ├── model/          # JPA entities (Cinema, Movie, Screening, etc.)
│   └── repository/     # Spring Data JPA repositories
├── save/               # Data ingestion modules
│   ├── cinemas/        # Cinema location importers
│   ├── movies/         # Film metadata importers
│   └── screenings/     # Screening showtime importers
└── search/
    ├── controller/     # REST controllers
    └── dto/            # Response DTOs
```

## License

This project is available for personal and educational use.
