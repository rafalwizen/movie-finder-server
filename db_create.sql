CREATE TABLE movies (
  id BIGINT PRIMARY KEY,
  originalId VARCHAR(255),
  title VARCHAR(255),
  original_title VARCHAR(255),
  year INT,
  duration_minutes INT,
  description TEXT,
  poster_url TEXT,
  created_at TIMESTAMP
);

CREATE TABLE cinemas (
  id BIGINT PRIMARY KEY,
  name VARCHAR(255),
  city VARCHAR(255),
  address VARCHAR(255),
  website_url TEXT,
  created_at TIMESTAMP
);

CREATE TABLE screenings (
  id BIGINT PRIMARY KEY,
  movie_id BIGINT,
  cinema_id BIGINT,
  screening_datetime TIMESTAMP,
  screening_url TEXT,
  created_at TIMESTAMP,
  FOREIGN KEY (movie_id) REFERENCES movies(id),
  FOREIGN KEY (cinema_id) REFERENCES cinemas(id)
);