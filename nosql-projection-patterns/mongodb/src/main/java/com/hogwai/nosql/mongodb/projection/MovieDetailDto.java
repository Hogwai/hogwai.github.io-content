package com.hogwai.nosql.mongodb.projection;

import java.util.List;

public record MovieDetailDto(
        String id,
        String title,
        int releaseYear,
        String genre,
        List<ActorDto> actors
) {

    public record ActorDto(
            String id,
            String firstName,
            String lastName,
            String bio,
            String birthDate,
            String nationality,
            List<String> awards,
            List<String> filmography
    ) {
    }
}
