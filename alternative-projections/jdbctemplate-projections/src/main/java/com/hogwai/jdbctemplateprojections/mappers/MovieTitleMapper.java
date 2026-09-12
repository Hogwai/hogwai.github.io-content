package com.hogwai.jdbctemplateprojections.mappers;

import com.hogwai.jdbctemplateprojections.dto.MovieTitleDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MovieTitleMapper implements RowMapper<MovieTitleDto> {
    @Override
    public MovieTitleDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MovieTitleDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getInt("release_year"),
                rs.getString("genre")
        );
    }
}
