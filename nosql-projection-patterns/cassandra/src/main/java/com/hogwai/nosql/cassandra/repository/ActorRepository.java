package com.hogwai.nosql.cassandra.repository;

import org.springframework.data.repository.CrudRepository;
import com.hogwai.nosql.cassandra.model.Actor;

public interface ActorRepository extends CrudRepository<Actor, String> {
}
