# Cinema API Documentation

## Base URL
```
http://localhost:8080
```

## General Notes

- All endpoints return JSON
- No CORS is configured — requests from other origins will be blocked by the browser
- When no results match, endpoints return an empty array `[]` (never `404` or `204`)
- `LocalDateTime` fields are serialized as ISO-8601 strings (e.g., `2026-02-20T18:30:00`). This relies on Jackson's default behavior — no explicit format is configured
- All response field names use `camelCase`

---

## Search Endpoints

### 1. Movies with Active Screenings

**Endpoint:** `GET /api/movies`

**Description:** Returns movies that have at least one screening in the database. Results are sorted alphabetically by title.

**Query Parameters:**
| Parameter | Required | Type | Default | Description |
|-----------|----------|------|---------|-------------|
| `q` | no | string | — | Title fragment to search for (case-insensitive) |

**Behavior:**
- Without `q`: returns all movies that have at least one screening
- With `q`: returns movies with active screenings whose title contains the query string

**Example Requests:**
```
GET /api/movies
GET /api/movies?q=avengers
GET /api/movies?q=spider
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Avengers: Endgame",
    "year": 2019,
    "posterUrl": "https://example.com/poster1.jpg"
  },
  {
    "id": 2,
    "title": "Spider-Man: No Way Home",
    "year": 2021,
    "posterUrl": "https://example.com/poster2.jpg"
  }
]
```

---

### 2. Screenings by Movie

**Endpoint:** `GET /api/screenings/by-movie`

**Description:** Returns screenings for a specific movie. Results are sorted by datetime ascending (earliest first).

**Query Parameters:**
| Parameter | Required | Type | Default | Description |
|-----------|----------|------|---------|-------------|
| `movieId` | **yes** | number (Long) | — | Movie ID |
| `includePast` | no | boolean | `false` | Whether to include past screenings |

**Behavior:**
- `includePast=false` (default): only screenings from now onwards
- `includePast=true`: all screenings (past and future)

**Example Requests:**
```
GET /api/screenings/by-movie?movieId=1
GET /api/screenings/by-movie?movieId=1&includePast=false
GET /api/screenings/by-movie?movieId=1&includePast=true
```

**Response:** `200 OK`
```json
[
  {
    "screeningDatetime": "2026-02-20T18:30:00",
    "cinemaName": "Cinema City",
    "cinemaCity": "Warsaw",
    "cinemaAddress": "59 Złota Street",
    "screeningUrl": "https://www.cinema-city.pl/screening/12345",
    "providerCode": "CINEMA_CITY"
  },
  {
    "screeningDatetime": "2026-02-20T21:00:00",
    "cinemaName": "Multikino",
    "cinemaCity": "Krakow",
    "cinemaAddress": "34 Podgórska Street",
    "screeningUrl": "https://www.multikino.pl/screening/67890",
    "providerCode": "MULTIKINO"
  }
]
```

**Error Responses:**
| Status | When |
|--------|------|
| `400 Bad Request` | `movieId` parameter is missing or is not a valid number |

---

### 3. All Movies

**Endpoint:** `GET /api/movies/allMovies`

**Description:** Returns all movies in the database, regardless of whether they have screenings. Results are sorted alphabetically by title.

**Example Request:**
```
GET /api/movies/allMovies
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Avengers: Endgame",
    "year": 2019,
    "posterUrl": "https://example.com/poster1.jpg"
  },
  {
    "id": 2,
    "title": "Spider-Man: No Way Home",
    "year": 2021,
    "posterUrl": "https://example.com/poster2.jpg"
  },
  {
    "id": 3,
    "title": "Batman Begins",
    "year": 2005,
    "posterUrl": "https://example.com/poster3.jpg"
  }
]
```

---

## Import Endpoints

All import endpoints are asynchronous — they return `202 Accepted` immediately and the import runs in the background.

### 4. Import Cinemas

**Endpoint:** `POST /api/import/cinemas`

**Response:** `202 Accepted`
```json
{
  "status": "cinema import started"
}
```

### 5. Import Films

**Endpoint:** `POST /api/import/films`

**Response:** `202 Accepted`
```json
{
  "status": "film import started"
}
```

### 6. Import Screenings

**Endpoint:** `POST /api/import/screenings`

**Response:** `202 Accepted`
```json
{
  "status": "screening import started"
}
```

### 7. Import All (Cinemas + Films + Screenings)

**Endpoint:** `POST /api/import/all`

**Response:** `202 Accepted`
```json
{
  "status": "full import started"
}
```

---

## Data Models

### MovieDTO
```json
{
  "id": "Long — unique movie identifier",
  "title": "string — movie title",
  "year": "Integer | null — release year, may be missing",
  "posterUrl": "string | null — URL to poster image, may be missing"
}
```

### ScreeningDTO
```json
{
  "screeningDatetime": "string (ISO 8601) — e.g. \"2026-02-20T18:30:00\"",
  "cinemaName": "string — cinema display name",
  "cinemaCity": "string | null — city name, may be missing",
  "cinemaAddress": "string | null — street address, may be missing",
  "screeningUrl": "string | null — URL to booking page, may be missing",
  "providerCode": "string — cinema chain code (e.g. \"CINEMA_CITY\", \"MULTIKINO\", \"HELIOS\")"
}
```

---

## Nullability Guide

The API does not enforce NOT NULL constraints on most fields. Frontend should defensively handle `null` values:

**Fields that can be `null`:**
| DTO | Field | Notes |
|-----|-------|-------|
| MovieDTO | `year` | Import may not always have year data |
| MovieDTO | `posterUrl` | Not all movies have poster URLs |
| ScreeningDTO | `cinemaCity` | Cinema may lack city data |
| ScreeningDTO | `cinemaAddress` | Cinema may lack address data |
| ScreeningDTO | `screeningUrl` | Not all screenings have booking URLs |

**Fields that should always be present:**
| DTO | Field |
|-----|-------|
| MovieDTO | `id`, `title` |
| ScreeningDTO | `screeningDatetime`, `cinemaName`, `providerCode` |

---

## Error Response Format

When an error occurs, Spring Boot returns a standard error body:

```json
{
  "timestamp": "2026-05-28T12:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Required request parameter 'movieId' is not present",
  "path": "/api/screenings/by-movie"
}
```

---

## Known Provider Codes

The `providerCode` field in `ScreeningDTO` identifies the cinema chain:

| Code | Chain |
|------|-------|
| `CINEMA_CITY` | Cinema City |
| `MULTIKINO` | Multikino |
| `HELIOS` | Helios |

---

## Usage Examples

### cURL

```bash
# Get all movies with active screenings
curl -X GET "http://localhost:8080/api/movies"

# Search movies by title fragment
curl -X GET "http://localhost:8080/api/movies?q=batman"

# Get all movies (including without screenings)
curl -X GET "http://localhost:8080/api/movies/allMovies"

# Get future screenings for a movie
curl -X GET "http://localhost:8080/api/screenings/by-movie?movieId=1"

# Get all screenings (including past)
curl -X GET "http://localhost:8080/api/screenings/by-movie?movieId=1&includePast=true"

# Trigger full data import
curl -X POST "http://localhost:8080/api/import/all"
```

### JavaScript (fetch)

```javascript
// Get all movies with active screenings
fetch('http://localhost:8080/api/movies')
  .then(response => response.json())
  .then(data => console.log(data));

// Search movies
fetch('http://localhost:8080/api/movies?q=batman')
  .then(response => response.json())
  .then(data => console.log(data));

// Get future screenings for a movie
fetch('http://localhost:8080/api/screenings/by-movie?movieId=1')
  .then(response => response.json())
  .then(data => console.log(data));

// Get all screenings (including past)
fetch('http://localhost:8080/api/screenings/by-movie?movieId=1&includePast=true')
  .then(response => response.json())
  .then(data => console.log(data));

// Trigger full data import
fetch('http://localhost:8080/api/import/all', { method: 'POST' })
  .then(response => response.json())
  .then(data => console.log(data));
```
