package com.hogwai.repository;

import com.hogwai.entity.ChainStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChainStatusRepository extends JpaRepository<ChainStatus, Integer> {
}
