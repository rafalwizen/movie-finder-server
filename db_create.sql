CREATE TABLE cinema_providers
(
    id   BIGINT PRIMARY KEY,
    code VARCHAR(50)  NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE movies
(
    id               BIGINT PRIMARY KEY,
    title            VARCHAR(255),
    original_title   VARCHAR(255),
    year             INT,
    duration_minutes INT,
    description      TEXT,
    poster_url       TEXT,
    created_at       TIMESTAMP
);

CREATE TABLE cinemas
(
    id                 BIGINT PRIMARY KEY,
    provider_id        BIGINT       NOT NULL,
    external_cinema_id VARCHAR(255) NOT NULL,
    name               VARCHAR(255),
    city               VARCHAR(255),
    address            VARCHAR(255),
    website_url        TEXT,
    latitude           REAL,
    longitude          REAL,
    created_at         TIMESTAMP,
    FOREIGN KEY (provider_id) REFERENCES cinema_providers (id),
    UNIQUE (provider_id, external_cinema_id)
);

CREATE TABLE movie_sources
(
    id                BIGINT PRIMARY KEY,
    movie_id          BIGINT       NOT NULL,
    provider_id       BIGINT       NOT NULL,
    external_movie_id VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies (id),
    FOREIGN KEY (provider_id) REFERENCES cinema_providers (id),
    UNIQUE (provider_id, external_movie_id)
);

CREATE TABLE screenings
(
    id                 BIGINT PRIMARY KEY,
    movie_id           BIGINT NOT NULL,
    cinema_id          BIGINT NOT NULL,
    screening_datetime TIMESTAMP,
    screening_url      TEXT,
    created_at         TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies (id),
    FOREIGN KEY (cinema_id) REFERENCES cinemas (id)
);