package com.hogwai.repository;

import com.hogwai.entity.ChainConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChainConfigurationRepository extends JpaRepository<ChainConfiguration, Integer> {
    Optional<ChainConfiguration> findByConfName(String confName);
}
