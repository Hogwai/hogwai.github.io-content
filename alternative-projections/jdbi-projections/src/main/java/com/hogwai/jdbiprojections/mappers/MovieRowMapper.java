package com.hogwai.jdbiprojections.mappers;

import com.hogwai.jdbiprojections.dto.MovieTitleDto;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MovieRowMapper implements RowMapper<MovieTitleDto> {

    @Override
    public MovieTitleDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new MovieTitleDto(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getInt("release_year"),
                rs.getString("genre")
        );
    }
}
