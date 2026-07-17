package com.sentinelai.sentinel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;
import static org.hibernate.generator.EventType.INSERT;

@Entity
@Table(name = "raw_signals")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class RawSignalEntity {

    @Id
    private UUID id;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name="occurred_at", nullable=false)
    private Instant occurredAt;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hints", columnDefinition = "jsonb")
    private Map<String, Object> hints;

    @Generated(event = INSERT)
    @Column(name="received_at", nullable = false, insertable = false, updatable = false)
    private Instant receivedAt;

    public RawSignalEntity(UUID id, String externalId, String source, Instant occurredAt, String message, Map<String, Object> hints) {
        this.id = id;
        this.externalId = externalId;
        this.source = source;
        this.occurredAt = occurredAt;
        this.message = message;
        this.hints = hints;
    }
}
