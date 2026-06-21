package com.hogwai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ts_chain_config")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChainConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chn_cfg_id", unique = true, updatable = false, nullable = false)
    private Integer id;

    @Column(name = "chn_cfg_name")
    private String confName;

    @Column(name = "chn_cfg_description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "chn_sts_id", insertable = false, updatable = false)
    private ChainStatus chainStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chn_id")
    private Chain chain;

    @OneToMany(mappedBy = "chainConfiguration", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<ChainStep> chainSteps;

    @Column(name = "chn_cfg_creation_date")
    private LocalDateTime creationDate;

    @Column(name = "chn_cfg_update_date")
    private LocalDateTime updateDate;
}
