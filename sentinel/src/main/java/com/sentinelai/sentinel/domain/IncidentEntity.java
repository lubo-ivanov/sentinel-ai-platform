package com.sentinelai.sentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

@Entity
@Table(name = "incidents")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class IncidentEntity {

    @Id
    private UUID id;

    @Setter
    @Column(nullable = false)
    private String title;

    @Setter
    @Column(nullable = false, length = 16)
    private String severity;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentStatus status;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Generated(event = INSERT)
    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Setter
    @Generated(event = INSERT)
    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Setter
    @Column(name = "anomaly_count", nullable = false)
    private Integer anomalyCount;

    @Generated(event = INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Generated(event = {INSERT, UPDATE})
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public IncidentEntity(UUID id, String title, String severity, IncidentStatus status, String fingerprint, Integer anomalyCount) {
        this.id = id;
        this.title = title;
        this.severity = severity;
        this.status = status;
        this.fingerprint = fingerprint;
        this.anomalyCount = anomalyCount;
    }

}
