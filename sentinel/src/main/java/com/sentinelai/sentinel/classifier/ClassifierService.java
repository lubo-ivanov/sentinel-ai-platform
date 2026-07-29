package com.sentinelai.sentinel.classifier;

import com.sentinelai.sentinel.domain.OperationalEventEntity;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import com.sentinelai.sentinel.repository.OperationalEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClassifierService {

    private final RuleEngine ruleEngine;
    private final OperationalEventRepository operationalEventRepository;

    public OperationalEvent classifyAndStore(RawSignalEntity signal) {
        OperationalEvent event = ruleEngine.classify(signal);
        operationalEventRepository.save(toEntity(event));
        return event;
    }

    private OperationalEventEntity toEntity(OperationalEvent event) {
        return new OperationalEventEntity(
                event.id(),
                event.sourceSignalId(),
                event.source(),
                event.timestamp(),
                event.type(),
                event.severity(),
                event.classification().method(),
                event.classification().ruleId(),
                event.classification().confidence(),
                event.payload()
        );
    }
}
