package com.hogwai.repository;

import com.hogwai.entity.ChainConfiguration;
import com.hogwai.entity.ChainStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChainStepRepository extends JpaRepository<ChainStep, Integer> {
    List<ChainStep> findAllByChainConfiguration(ChainConfiguration chainConfiguration);

    @Query("""
        select chs
        from ChainStep chs
        where chs.currentStep.stepName = :stepName
        and chs.chainConfiguration.confName = :confName
        """)
    Optional<ChainStep> findByStepAndConfiguration(@Param("stepName") String stepName,
                                                   @Param("confName") String confName);

    void deleteAllByChainConfiguration(ChainConfiguration chainConfiguration);
}
