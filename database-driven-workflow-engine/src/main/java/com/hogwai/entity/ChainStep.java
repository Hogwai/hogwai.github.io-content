package com.hogwai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ts_chain_step")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChainStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chn_stp_id", unique = true, updatable = false, nullable = false)
    private Integer id;

    @Column(name = "chn_stp_next_step_on_success")
    private String nextStepOnSuccess;

    @Column(name = "chn_stp_next_step_on_failure")
    private String nextStepOnFailure;

    @ManyToOne
    @JoinColumn(name = "chn_sts_id", insertable = false, updatable = false)
    private ChainStatus chainStatus;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chn_cfg_id")
    private ChainConfiguration chainConfiguration;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "stp_id")
    private Step currentStep;

    @Column(name = "chn_stp_creation_date")
    private LocalDateTime creationDate;

    @Column(name = "chn_stp_update_date")
    private LocalDateTime updateDate;
}
