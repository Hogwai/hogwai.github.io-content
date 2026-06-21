package com.hogwai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ts_step")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Step {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stp_id", unique = true, updatable = false, nullable = false)
    private Integer id;

    @Column(name = "stp_name")
    private String stepName;

    @Column(name = "stp_description")
    private String description;

    @Column(name = "stp_creation_date")
    private LocalDateTime creationDate;

    @Column(name = "stp_update_date")
    private LocalDateTime updateDate;
}
