package com.hogwai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tr_chain_status")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChainStatus {
    @Id
    @Column(name = "chn_sts_id", unique = true, updatable = false, nullable = false)
    private Integer id;

    @Column(name = "chn_sts_name")
    private String chainStatusName;

    @Column(name = "chn_sts_creation_date")
    private LocalDateTime creationDate;

    @Column(name = "chn_sts_update_date")
    private LocalDateTime updateDate;
}
