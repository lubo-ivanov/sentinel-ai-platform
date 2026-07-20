package com.sentinelai.sentinel.domain;

import com.sentinelai.sentinel.classifier.FailureType;
import com.sentinelai.sentinel.classifier.OperationalEvent;
import com.sentinelai.sentinel.classifier.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.type.SqlTypes.JSON;

@Entity
@Table(name = "operational_events")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class OperationalEventEntity {

    @Id
    private UUID id;

    @Column(name = "source_signal_id", nullable = false)
    private UUID sourceSignalId;

    @Column(nullable = false, length = 64)
    private String source;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Enumerated(STRING)
    @Column(nullable = false, length = 64)
    private FailureType type;

    @Enumerated(STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Enumerated(STRING)
    @Column(name = "classification_method", nullable = false, length = 16)
    private OperationalEvent.Classification.Method classificationMethod;

    @Column(name = "classification_rule_id", length = 128)
    private String classificationRuleId;

    @Column(name = "classification_confidence", nullable = false)
    private double classificationConfidence;

    @JdbcTypeCode(JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Generated(event = INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public OperationalEventEntity(
            UUID id,
            UUID sourceSignalId,
            String source,
            Instant timestamp,
            FailureType type,
            Severity severity,
            OperationalEvent.Classification.Method classificationMethod,
            String classificationRuleId,
            double classificationConfidence,
            Map<String, Object> payload
    ) {
        this.id = id;
        this.sourceSignalId = sourceSignalId;
        this.source = source;
        this.timestamp = timestamp;
        this.type = type;
        this.severity = severity;
        this.classificationMethod = classificationMethod;
        this.classificationRuleId = classificationRuleId;
        this.classificationConfidence = classificationConfidence;
        this.payload = payload;
    }
}
