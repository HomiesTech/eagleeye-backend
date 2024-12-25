package com.homenetics.eagleeye.repository;

import com.homenetics.eagleeye.entity.DBEntity.AlarmsDBEntity;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AlarmsRepository extends JpaRepository<AlarmsDBEntity, Integer> {
    @Modifying
    @Transactional
    @Query("INSERT INTO alarms (entity_type, entity_id, severity, start_time, duration, key, status, detail, last_updated_time, resolution_time, state) " +
            "VALUES (:entityType, :entityId, :severity, :startTime, :duration, :key, :status, :detail, :lastUpdatedTime, :resolutionTime, :state)")
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

    @Modifying
    @Transactional
    @Query("SELECT alarm_id FROM alarms a WHERE a.entity_type = :entityType AND a.entity_id = :entityId AND a.key = :key AND a.state = 1")
    Integer getActiveAlarm(@Param("entityType") String entityType,@Param("entityId") Integer entityId,@Param("key") String key);

    @Modifying
    @Transactional
    @Query("UPDATE alarms SET resolution_time = :resolutionTime, state = 0 WHERE entity_type = :entityType AND entity_id = :entityId AND key = :key")
    void updateAlarmState(@Param("entityType") String entityType,@Param("entityId") Integer entityId,@Param("key") String key, @Param("resolutionTime") LocalDateTime resolutionTime);

}
