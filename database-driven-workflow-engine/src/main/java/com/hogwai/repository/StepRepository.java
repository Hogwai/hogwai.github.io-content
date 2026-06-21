package com.hogwai.repository;

import com.hogwai.dto.StepRecord;
import com.hogwai.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StepRepository extends JpaRepository<Step, Integer> {
    Optional<Step> findByStepName(String stepName);

    @Query("select new com.hogwai.dto.StepRecord(st.stepName, st.description) from Step st")
    List<StepRecord> findAllStepsForChainConfigurations();

    boolean existsByStepName(String stepName);
}
