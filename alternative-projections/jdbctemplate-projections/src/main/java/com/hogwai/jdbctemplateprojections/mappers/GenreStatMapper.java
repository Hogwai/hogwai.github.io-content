package com.hogwai.jdbctemplateprojections.mappers;

import com.hogwai.jdbctemplateprojections.dto.GenreStatDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GenreStatMapper implements RowMapper<GenreStatDto> {
    @Override
    public GenreStatDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new GenreStatDto(
                rs.getString("genre"),
                rs.getLong("movie_count")
        );
    }
}
