package com.hogwai.jdbiprojections.config;

import com.hogwai.jdbiprojections.dto.GenreStatDto;
import com.hogwai.jdbiprojections.dto.MovieWithActorsDto;
import com.hogwai.jdbiprojections.mappers.MovieRowMapper;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.spring.EnableJdbiRepositories;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableJdbiRepositories(basePackages = "com.hogwai.jdbiprojections.repository")
public class JdbiConfig {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        return Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin())
                .installPlugin(new PostgresPlugin())
                .registerRowMapper(new MovieRowMapper())
                .registerRowMapper(ConstructorMapper.factory(GenreStatDto.class))
                .registerRowMapper(ConstructorMapper.factory(MovieWithActorsDto.ActorDto.class));
    }
}
