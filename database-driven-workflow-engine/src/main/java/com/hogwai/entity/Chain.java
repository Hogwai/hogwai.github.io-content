package com.hogwai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ts_chain")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Chain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chn_id", unique = true, updatable = false, nullable = false)
    private Integer id;

    @Column(name = "chn_name")
    private String chainName;

    @Column(name = "chn_description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "chn_sts_id", insertable = false, updatable = false)
    private ChainStatus status;

    @Column(name = "chn_creation_date")
    private LocalDateTime creationDate;

    @Column(name = "chn_update_date")
    private LocalDateTime updateDate;

    @OneToMany(mappedBy = "chain", cascade = CascadeType.ALL)
    private List<ChainConfiguration> chainConfigurations;
}
