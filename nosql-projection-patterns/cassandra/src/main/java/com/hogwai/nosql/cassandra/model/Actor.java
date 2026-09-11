package com.hogwai.nosql.cassandra.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("actors")
public class Actor {

    @PrimaryKey
    private String id;
    private String firstName;
    private String lastName;
    private String bio;
    private String birthDate;
    private String nationality;
    private String awards;      // Denormalized: comma-separated
    private String filmography; // Denormalized: comma-separated

    public Actor() {
    }

    public Actor(String id, String firstName, String lastName, String bio, String birthDate,
                 String nationality, String awards, String filmography) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bio = bio;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.awards = awards;
        this.filmography = filmography;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public String getAwards() { return awards; }
    public void setAwards(String awards) { this.awards = awards; }
    public String getFilmography() { return filmography; }
    public void setFilmography(String filmography) { this.filmography = filmography; }
}
