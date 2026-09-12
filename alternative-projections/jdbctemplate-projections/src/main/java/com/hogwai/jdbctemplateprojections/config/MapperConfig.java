package com.hogwai.jdbctemplateprojections.config;

import com.hogwai.jdbctemplateprojections.mappers.GenreStatMapper;
import com.hogwai.jdbctemplateprojections.mappers.MovieTitleMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public MovieTitleMapper movieTitleMapper() {
        return new MovieTitleMapper();
    }

    @Bean
    public GenreStatMapper genreStatMapper() {
        return new GenreStatMapper();
    }
}
