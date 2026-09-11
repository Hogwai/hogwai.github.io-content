package com.hogwai.nosql.mongodb;

import java.util.UUID;
import java.util.random.RandomGenerator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hogwai.nosql.mongodb.model.Actor;
import com.hogwai.nosql.mongodb.model.Movie;
import com.hogwai.nosql.mongodb.repository.MovieRepository;

@Component
public class MongoDataInitializer implements CommandLineRunner {

    private final MovieRepository movieRepository;

    public MongoDataInitializer(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            return;
        }

        String[] titles = {
            "The Dark Knight", "Inception", "Interstellar", "Pulp Fiction", "Goodfellas",
            "The Godfather", "Fight Club", "Forrest Gump", "The Matrix", "Gladiator",
            "Titanic", "Avatar", "The Avengers", "Jurassic Park", "Star Wars",
            "Lord of the Rings", "The Shawshank Redemption", "Schindler's List",
            "Saving Private Ryan", "Braveheart", "The Departed", "Casino",
            "Heat", "Scarface", "Carlito's Way", "Donnie Brasco", "The Untouchables",
            "A Bronx Tale", "Mean Streets", "Raging Bull", "Taxi Driver",
            "The King of Comedy", "After Hours", "Midnight Run", "Cop Land",
            "Ronin", "The Third Man", "Strangers on a Train", "Rear Window",
            "Vertigo", "North by Northwest", "Psycho", "The Birds", "Dial M for Murder"
        };

        String[] firstNames = {
            "Robert", "Al", "Joe", "Denzel", "Harrison", "Mark", "Carrie", "Christian",
            "Heath", "Leonardo", "Tom", "Matthew", "Samuel", "John", "Uma", "Jim",
            "Kate", "Brad", "Morgan", "Anthony", "Jack", "Nick", "Dustin", "Sylvester",
            "Arnold", "Bruce", "Jean-Claude", "Chuck", "Wesley", "Jet", "Donnie",
            "Jason", "Keanu", "Will", "Matt", "Ben", "George", "Clint", "Steven",
            "Martin", "Stanley", "David", "Ridley", "James", "Peter", "Quentin"
        };

        String[] lastNames = {
            "De Niro", "Pacino", "Pesci", "Washington", "Ford", "Hamill", "Fisher",
            "Bale", "Ledger", "DiCaprio", "Hanks", "McConaughey", "Jackson", "Travolta",
            "Thurman", "Carrey", "Winslet", "Pitt", "Freeman", "Hopkins", "Nicholson",
            "Cage", "Pesci", "Stallone", "Schwarzenegger", "Willis", "Van Damme",
            "Norris", "Snipes", "Li", "Yun", "Statham", "Reeves", "Smith",
            "Damon", "Affleck", "Clooney", "Eastwood", "Spielberg", "Scorsese",
            "Kubrick", "Lynch", "Scott", "Cameron", "Jackson", "Tarantino"
        };

        String[] genres = {"Crime", "Action", "Sci-Fi", "Drama", "Thriller", "Comedy"};

        String[] nationalities = {"American", "British", "Australian", "Canadian", "French", "German", "Italian", "Irish", "Scottish", "New Zealander"};

        String[] awardTemplates = {
            "Academy Award for Best Actor", "Golden Globe for Best Actor", "BAFTA for Best Actor",
            "Screen Actors Guild Award", "Critics Choice Award", "Independent Spirit Award",
            "Saturn Award for Best Actor", "MTV Movie Award", "People's Choice Award",
            "Hollywood Film Award", "Palm d'Or Nomination", "Cannes Best Actor"
        };

        String[] moviePool = {
            "Goodfellas", "Casino", "The Godfather", "Heat", "Scarface", "Taxi Driver",
            "Raging Bull", "The Dark Knight", "Inception", "Interstellar", "Fight Club",
            "The Matrix", "Gladiator", "Braveheart", "Saving Private Ryan", "Forrest Gump",
            "Titanic", "Avatar", "The Avengers", "Star Wars", "Pulp Fiction",
            "The Departed", "Donnie Brasco", "Carlito's Way", "A Bronx Tale",
            "Mean Streets", "The Untouchables", "Ronin", "Cop Land", "Midnight Run",
            "The Third Man", "Rear Window", "Vertigo", "Psycho", "North by Northwest",
            "The Shawshank Redemption", "Schindler's List", "The Lord of the Rings",
            "Harry Potter", "The Bourne Identity", "Mission Impossible", "Die Hard",
            "Lethal Weapon", "Rocky", "Rambo", "Terminator", "Predator",
            "The Terminator", "Alien", "Blade Runner", "The Truman Show"
        };

        RandomGenerator random = RandomGenerator.of("L64X128MixRandom");

        // Generate 500 unique actors with rich data
        java.util.Map<String, Actor> actorMap = new java.util.HashMap<>();
        for (int i = 0; i < 500; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];

            // Generate bio (~200-300 chars)
            String bio = firstName + " " + lastName + " is a renowned actor known for his work in cinema. "
                + "Born in " + nationalities[random.nextInt(nationalities.length)].toLowerCase() + ", "
                + firstName + " has appeared in numerous critically acclaimed films throughout his career. "
                + "His versatility as an actor has earned him recognition from audiences and critics worldwide. "
                + "With a career spanning over three decades, " + firstName + " continues to be a dominant force in Hollywood.";

            // Generate filmography (5-10 movies)
            int filmCount = 5 + random.nextInt(6);
            java.util.List<String> filmography = new java.util.ArrayList<>();
            for (int j = 0; j < filmCount; j++) {
                filmography.add(moviePool[random.nextInt(moviePool.length)]);
            }

            // Generate awards (2-5)
            int awardCount = 2 + random.nextInt(4);
            java.util.List<String> awards = new java.util.ArrayList<>();
            for (int j = 0; j < awardCount; j++) {
                awards.add(awardTemplates[random.nextInt(awardTemplates.length)]);
            }

            String id = UUID.randomUUID().toString();
            Actor actor = new Actor(
                id,
                firstName,
                lastName,
                bio,
                (1950 + random.nextInt(50)) + "-" + String.format("%02d", 1 + random.nextInt(12)) + "-" + String.format("%02d", 1 + random.nextInt(28)),
                nationalities[random.nextInt(nationalities.length)],
                awards,
                filmography
            );
            actorMap.put(id, actor);
        }

        java.util.List<Actor> allActors = new java.util.ArrayList<>(actorMap.values());

        // Generate 50000 movies with 8-12 rich actors each
        for (int i = 0; i < 50_000; i++) {
            String title = titles[random.nextInt(titles.length)] + " " + (i + 1);
            int year = 1970 + random.nextInt(55);
            String genre = genres[random.nextInt(genres.length)];

            int actorCount = 8 + random.nextInt(5);
            java.util.List<Actor> actors = new java.util.ArrayList<>();
            for (int j = 0; j < actorCount; j++) {
                actors.add(allActors.get(random.nextInt(allActors.size())));
            }

            movieRepository.save(new Movie(
                UUID.randomUUID().toString(),
                title,
                year,
                genre,
                actors
            ));
        }
    }
}
