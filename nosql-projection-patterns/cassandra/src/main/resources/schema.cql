CREATE KEYSPACE IF NOT EXISTS projections
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE IF NOT EXISTS projections.movies (
    id text PRIMARY KEY,
    title text,
    release_year int,
    genre text,
    actor_ids text
);

CREATE TABLE IF NOT EXISTS projections.actors (
    id text PRIMARY KEY,
    first_name text,
    last_name text,
    bio text,
    birth_date text,
    nationality text,
    awards text,
    filmography text
);

-- Secondary indexes for query patterns
CREATE INDEX IF NOT EXISTS movies_genre_idx ON projections.movies (genre);
CREATE INDEX IF NOT EXISTS movies_title_idx ON projections.movies (title);
