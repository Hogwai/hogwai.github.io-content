package com.hogwai.nosql.cassandra.projection;

import java.util.List;

public record MovieDetailDto(
        String id,
        String title,
        int releaseYear,
        String genre,
        List<ActorDetailDto> actors
) {

    public record ActorDetailDto(
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
