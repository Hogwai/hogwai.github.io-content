package com.hogwai.repository;

import com.hogwai.entity.Chain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChainRepository extends JpaRepository<Chain, Integer> {
    Optional<Chain> findByChainName(String chainName);
}
