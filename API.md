# Cinema API Documentation

## Base URL
```
http://localhost:8080
```

## Endpoints

### 1. Movies List (autocomplete / selection)

**Endpoint:** `GET /api/movies`

**Description:** Returns a list of movies that have active screenings.

**Query Parameters:**
- `q` (optional) - movie title fragment to search for

**Example Requests:**
```bash
GET /api/movies
GET /api/movies?q=avengers
GET /api/movies?q=spider
```

**Response:**
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

**Status Codes:**
- `200 OK` - request completed successfully

---

### 2. Movie Screenings by Movie ID

**Endpoint:** `GET /api/screenings/by-movie`

**Description:** Returns a list of screenings for a movie with the given ID.

**Query Parameters:**
- `movieId` (required) - movie ID
- `includePast` (optional, default: `false`) - whether to include past screenings
  - `false` - returns only future screenings (from now onwards)
  - `true` - returns all screenings (past and future)

**Example Requests:**
```bash
GET /api/screenings/by-movie?movieId=1
GET /api/screenings/by-movie?movieId=1&includePast=false
GET /api/screenings/by-movie?movieId=1&includePast=true
```

**Response:**
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

**Status Codes:**
- `200 OK` - request completed successfully
- `400 Bad Request` - missing required `movieId` parameter

---

### 3. All Movies

**Endpoint:** `GET /api/allMovies`

**Description:** Returns a list of all movies in the database, regardless of whether they have active screenings or not.

**Example Requests:**
```bash
GET /api/allMovies
```

**Response:**
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

## Data Models

### MovieDTO
```json
{
  "id": "number",
  "title": "string",
  "year": "number",
  "posterUrl": "string"
}
```

### ScreeningDTO
```json
{
  "screeningDatetime": "string (ISO 8601 datetime)",
  "cinemaName": "string",
  "cinemaCity": "string",
  "cinemaAddress": "string",
  "screeningUrl": "string",
  "providerCode": "string"
}
```

---

## Usage Examples

### cURL

**Get all movies:**
```bash
curl -X GET "http://localhost:8080/api/movies"
```

**Search movies by title fragment:**
```bash
curl -X GET "http://localhost:8080/api/movies?q=batman"
```

**Find future screenings for a movie:**
```bash
curl -X GET "http://localhost:8080/api/screenings/by-movie?movieId=1"
```

**Find all screenings (including past) for a movie:**
```bash
curl -X GET "http://localhost:8080/api/screenings/by-movie?movieId=1&includePast=true"
```

### JavaScript (fetch)

```javascript
// Get all movies
fetch('http://localhost:8080/api/movies')
  .then(response => response.json())
  .then(data => console.log(data));

// Search movies
fetch('http://localhost:8080/api/movies?q=batman')
  .then(response => response.json())
  .then(data => console.log(data));

// Find future screenings for a movie
fetch('http://localhost:8080/api/screenings/by-movie?movieId=1')
  .then(response => response.json())
  .then(data => console.log(data));

// Find all screenings (including past)
fetch('http://localhost:8080/api/screenings/by-movie?movieId=1&includePast=true')
  .then(response => response.json())
  .then(data => console.log(data));
```

---

## Notes

- All endpoints return data in JSON format
- The `/api/movies` endpoint returns only movies that have at least one active screening in the database
- The `/api/screenings/by-movie` endpoint by default returns only future screenings (from current datetime onwards)
- Set `includePast=true` to retrieve all screenings including those that already occurred
- Dates and times are returned in ISO 8601 format (e.g., `2026-01-20T18:30:00`)
- Screenings are sorted by datetime in ascending order (earliest first)
- The `providerCode` field identifies the cinema provider (e.g., "CINEMA_CITY", "MULTIKINO")