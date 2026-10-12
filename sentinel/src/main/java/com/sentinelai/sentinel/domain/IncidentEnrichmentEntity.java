package com.sentinelai.sentinel.domain;

import com.sentinelai.sentinel.llm.AiSummaryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "incident_enrichments")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class IncidentEnrichmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false, unique = true)
    private UUID incidentId;

    @Setter
    @Column(name = "ai_summary")
    private String aiSummary;

    @Setter
    @Column(name = "ai_likely_cause")
    private String aiLikelyCause;

    @Setter
    @Column(name = "remediation_steps")
    private String remediationSteps;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_summary_status", nullable = false, length = 16)
    private AiSummaryStatus aiSummaryStatus;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Version
    @Column(nullable = false)
    private Long version;


    public IncidentEnrichmentEntity(UUID incidentId, AiSummaryStatus status) {
        this.incidentId = incidentId;
        this.aiSummaryStatus = status;
        this.generatedAt = Instant.now();
    }

}
