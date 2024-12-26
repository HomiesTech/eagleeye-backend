package com.homenetics.eagleeye.repository;

import com.homenetics.eagleeye.entity.DBEntity.AlarmsDBEntity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AlarmsRepository extends JpaRepository<AlarmsDBEntity, Integer>, JpaSpecificationExecutor<AlarmsDBEntity> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO alarms (entity_type, entity_id, severity, start_time, duration, alarm_key, status, detail, last_updated_time, resolution_time, state) " +
            "VALUES (:entityType, :entityId, :severity, :startTime, :duration, :key, :status, :detail, :lastUpdatedTime, :resolutionTime, :state)", nativeQuery = true)
    void addAlarm(@Param("entityType") String entityType,
                  @Param("entityId") Integer entityId,
                  @Param("severity") Integer severity,
                  @Param("startTime") LocalDateTime startTime,
                  @Param("duration") Long duration,
                  @Param("key") String key,
                  @Param("status") String status,
                  @Param("detail") String detail,
                  @Param("lastUpdatedTime") LocalDateTime lastUpdatedTime,
                  @Param("state") Boolean state);

    @Query(value = "SELECT * FROM alarms a WHERE a.entity_type = :entityType AND a.entity_id = :entityId AND a.alarm_key = :key AND a.state = 1", nativeQuery = true)
    AlarmsDBEntity getActiveAlarm(@Param("entityType") String entityType,@Param("entityId") Integer entityId,@Param("key") String key);

    // Move alarm to history
    @Modifying
    @Transactional
    @Query(value = "UPDATE alarms SET resolution_time = :resolutionTime, state = 0 WHERE entity_type = :entityType AND entity_id = :entityId AND alarm_key = :key", nativeQuery = true)
    void updateAlarmState(@Param("entityType") String entityType,@Param("entityId") Integer entityId,@Param("key") String key, @Param("resolutionTime") LocalDateTime resolutionTime);

    Page<AlarmsDBEntity> findByStateAndSeverityIn(Boolean state, List<Integer> severity, Pageable pageable);

}
