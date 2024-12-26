package com.homenetics.eagleeye.entity.DBEntity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name="alarms")
@Data
public class AlarmsDBEntity {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer alarmId;

    @Column
    private String entityType;

    @Column
    private Integer entityId;

    @Column
    private Integer severity;

    @Column
    private LocalDateTime startTime;

    @Column
    private Long duration;

    @Column
    private String alarmKey;

    @Column 
    private String status;

    @Column
    private String detail;

    @Column
    private LocalDateTime lastUpdatedTime;

    @Column
    private LocalDateTime resolutionTime;

    @Column (columnDefinition = "BIT DEFAULT 0")
    private boolean state;
}
