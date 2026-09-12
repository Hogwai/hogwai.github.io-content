package com.hogwai.querydslprojections.dto;

import java.util.List;

public record MovieWithActorsDto(Long id, String title, int releaseYear, String genre, List<ActorDto> actors) {

    public record ActorDto(Long id, String firstName, String lastName) {
    }
}
