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

The Docker setup mounts `./db` as a volume for persistent SQLite storage and enables scheduled daily imports by default.

## API Endpoints

### Search

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/movies` | Movies with active screenings (optional `?q=` title search) |
| `GET` | `/api/allMovies` | All movies regardless of screenings |
| `GET` | `/api/screenings/by-movie?movieId=` | Screenings for a movie (optional `&includePast=true`) |

### Import

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/import/cinemas` | Trigger cinema import (async) |
| `POST` | `/api/import/films` | Trigger film import (async) |
| `POST` | `/api/import/screenings` | Trigger screening import (async) |
| `POST` | `/api/import/all` | Trigger full import (async) |

All import endpoints return `202 Accepted` immediately and run in the background.

### Example

```bash
# Search movies
curl "http://localhost:8080/api/movies?q=batman"

# Get screenings
curl "http://localhost:8080/api/screenings/by-movie?movieId=1"

# Trigger full import
curl -X POST "http://localhost:8080/api/import/all"
```

Full API documentation: [API.md](API.md)

## Data Import

Data can be imported in three ways:

### 1. REST API (recommended)

Trigger imports at runtime via the `/api/import/*` endpoints (see above).

### 2. Scheduled import

Enable automatic daily import by setting properties:

```properties
import.scheduled.enabled=true
import.scheduled.cron=0 0 3 * * *   # default: every day at 3:00 AM
```

In Docker, this is enabled by default via `IMPORT_SCHEDULED_ENABLED=true`.

### 3. Spring profiles (one-shot)

```bash
# Fetch cinema locations
./mvnw spring-boot:run -Dspring-boot.run.profiles=fetch-cinemas

# Download film metadata
./mvnw spring-boot:run -Dspring-boot.run.profiles=download-films

# Fetch screening showtimes
./mvnw spring-boot:run -Dspring-boot.run.profiles=fetch-screenings
```

Each profile triggers a one-shot import that persists data to SQLite.

## GSC Daily Report (Telegram)

Optional daily Google Search Console → Telegram report (`gsc/` package). Sends
clicks/impressions/CTR/position for the last 7 days (vs. the previous 7),
top queries/pages and the indexing status of monitored URLs to a Telegram chat.

```properties
gsc.report.enabled=true                      # enable the daily schedule
gsc.report.cron=0 0 8 * * *                  # default: 8:00 Europe/Warsaw
gsc.report.site-url=https://gdzie-do-kina.pl/
gsc.report.monitored-urls=https://gdzie-do-kina.pl/   # comma-separated list
gsc.report.credentials-json=...              # service-account key (single-line JSON) OR:
gsc.report.credentials-file=...              # path to the key file (exactly one of the two)
telegram.bot-token=...
telegram.chat-id=...
```

One-shot manual run (no schedule needed):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=gsc-report
```

Setup guide (service account + BotFather) lives in `wdrozenie.txt`.

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
├── gsc/                # Daily GSC -> Telegram report
└── search/
    ├── controller/     # REST controllers
    └── dto/            # Response DTOs
```

## License

This project is available for personal and educational use.
