package com.hogwai.nosql.mongodb.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Actor {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String bio;
    private String birthDate;
    private String nationality;
    private List<String> awards;
    private List<String> filmography;

    public Actor() {
    }

    public Actor(String id, String firstName, String lastName, String bio, String birthDate,
                 String nationality, List<String> awards, List<String> filmography) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bio = bio;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.awards = awards;
        this.filmography = filmography;
    }

    public Actor(String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
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
    public List<String> getAwards() { return awards; }
    public void setAwards(List<String> awards) { this.awards = awards; }
    public List<String> getFilmography() { return filmography; }
    public void setFilmography(List<String> filmography) { this.filmography = filmography; }
}
