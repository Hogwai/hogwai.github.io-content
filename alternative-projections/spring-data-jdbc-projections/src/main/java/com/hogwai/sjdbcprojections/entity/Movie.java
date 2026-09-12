package com.hogwai.sjdbcprojections.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("movies")
public record Movie(@Id Long id, String title, int releaseYear, String genre) {
}
