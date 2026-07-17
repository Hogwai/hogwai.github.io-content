-- Movies
INSERT INTO movies (title, release_year, genre) VALUES ('The Shawshank Redemption', 1994, 'Drama');
INSERT INTO movies (title, release_year, genre) VALUES ('The Godfather', 1972, 'Crime');
INSERT INTO movies (title, release_year, genre) VALUES ('Pulp Fiction', 1994, 'Crime');
INSERT INTO movies (title, release_year, genre) VALUES ('Forrest Gump', 1994, 'Drama');
INSERT INTO movies (title, release_year, genre) VALUES ('Inception', 2010, 'Sci-Fi');
INSERT INTO movies (title, release_year, genre) VALUES ('The Matrix', 1999, 'Sci-Fi');
INSERT INTO movies (title, release_year, genre) VALUES ('Goodfellas', 1990, 'Crime');
INSERT INTO movies (title, release_year, genre) VALUES ('Interstellar', 2014, 'Sci-Fi');
INSERT INTO movies (title, release_year, genre) VALUES ('The Dark Knight', 2008, 'Action');
INSERT INTO movies (title, release_year, genre) VALUES ('Fight Club', 1999, 'Drama');

-- Actors
INSERT INTO actors (first_name, last_name) VALUES ('Tim', 'Robbins');
INSERT INTO actors (first_name, last_name) VALUES ('Morgan', 'Freeman');
INSERT INTO actors (first_name, last_name) VALUES ('Marlon', 'Brando');
INSERT INTO actors (first_name, last_name) VALUES ('Al', 'Pacino');
INSERT INTO actors (first_name, last_name) VALUES ('John', 'Travolta');
INSERT INTO actors (first_name, last_name) VALUES ('Samuel L.', 'Jackson');
INSERT INTO actors (first_name, last_name) VALUES ('Tom', 'Hanks');
INSERT INTO actors (first_name, last_name) VALUES ('Leonardo', 'DiCaprio');
INSERT INTO actors (first_name, last_name) VALUES ('Keanu', 'Reeves');
INSERT INTO actors (first_name, last_name) VALUES ('Robert', 'De Niro');
INSERT INTO actors (first_name, last_name) VALUES ('Ray', 'Liotta');
INSERT INTO actors (first_name, last_name) VALUES ('Christian', 'Bale');
INSERT INTO actors (first_name, last_name) VALUES ('Brad', 'Pitt');
INSERT INTO actors (first_name, last_name) VALUES ('Laurence', 'Fishburne');
INSERT INTO actors (first_name, last_name) VALUES ('Carrie-Anne', 'Moss');
INSERT INTO actors (first_name, last_name) VALUES ('Ellen', 'Burstyn');
INSERT INTO actors (first_name, last_name) VALUES ('Gary', 'Oldman');
INSERT INTO actors (first_name, last_name) VALUES ('Michael', 'Caine');
INSERT INTO actors (first_name, last_name) VALUES ('Robin', 'Wright');
INSERT INTO actors (first_name, last_name) VALUES ('Sally', 'Field');
INSERT INTO actors (first_name, last_name) VALUES ('Joseph', 'Gordon-Levitt');
INSERT INTO actors (first_name, last_name) VALUES ('Ken', 'Watanabe');

-- Movie-Actor relationships
-- Shawshank Redemption: Tim Robbins, Morgan Freeman
INSERT INTO movies_actors (movie_id, actor_id) VALUES (1, 1);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (1, 2);

-- The Godfather: Marlon Brando, Al Pacino
INSERT INTO movies_actors (movie_id, actor_id) VALUES (2, 3);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (2, 4);

-- Pulp Fiction: John Travolta, Samuel L. Jackson
INSERT INTO movies_actors (movie_id, actor_id) VALUES (3, 5);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (3, 6);

-- Forrest Gump: Tom Hanks, Robin Wright, Sally Field
INSERT INTO movies_actors (movie_id, actor_id) VALUES (4, 7);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (4, 19);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (4, 20);

-- Inception: Leonardo DiCaprio, Joseph Gordon-Levitt, Ken Watanabe, Michael Caine
INSERT INTO movies_actors (movie_id, actor_id) VALUES (5, 8);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (5, 21);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (5, 22);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (5, 18);

-- The Matrix: Keanu Reeves, Laurence Fishburne, Carrie-Anne Moss
INSERT INTO movies_actors (movie_id, actor_id) VALUES (6, 9);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (6, 14);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (6, 15);

-- Goodfellas: Robert De Niro, Ray Liotta
INSERT INTO movies_actors (movie_id, actor_id) VALUES (7, 10);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (7, 11);

-- Interstellar: Michael Caine only (McConaughey not in actors table)
INSERT INTO movies_actors (movie_id, actor_id) VALUES (8, 18);

-- The Dark Knight: Christian Bale, Michael Caine, Gary Oldman
INSERT INTO movies_actors (movie_id, actor_id) VALUES (9, 12);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (9, 18);
INSERT INTO movies_actors (movie_id, actor_id) VALUES (9, 17);

-- Fight Club: Brad Pitt
INSERT INTO movies_actors (movie_id, actor_id) VALUES (10, 13);
