package com.homenetics.eagleeye.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AlarmEntity {
    private Integer aid; // Unique alarm identifier
    private Integer severity; // error-4, ok-2, info-1, warn-3
    private LocalDateTime startTime;
    private Long duration;
    private String key; // Alarm key (e.g., dev.online, dev.msg.status)
    private String status; 
    private String detail;
    private LocalDateTime lastUpdatedTime;
    private String entityType; // "device,cust,mach"
    private Integer entityId;
    private LocalDateTime resolutionTime;
    private boolean state; // active - true, resolved/history - false
}

// * dev.msg.last_sync <time>
// * dev.msg.url <url>

/**
 * Alarm Keys:
 * dev.file.active yes/unknwon/no <yes when received from file <= 3min, unknown when > 3min < 5 min, no when > 5min>
 * dev.online yes/failed/no <failed means in file online, in db offline, warn for 3 mins, error after 3 mins>
 * dev.msgurl.rscode <rescode> <-99/200/else>
 * dev.btime.rscode <rescode> <-99/200/else>
 * dev.msg.status ok/not-ok <not-ok means, dev is connected to mqtt server, but msg is not received to mqtt>
 * dev.signal.strength <strength> <warn when poor>, <error when extreme poor>
 * dev.cred.username ok/not-ok <warn when not ok, means username is null>
 * dev.ota.status ok/not-ok <ota try and ota ok user mismatch>
 * dev.cred.status ok/not-ok <cred try and cred ok user mismatch>
 * dev.flash.status ok/not-ok <not ok when greater then 30%>
 * dev.nvs.status ok/not-ok <not ok when greater then 70%>
 * dev.time.millis ok/not-ok <not ok when dev restarted and prev time > current time>
 */
