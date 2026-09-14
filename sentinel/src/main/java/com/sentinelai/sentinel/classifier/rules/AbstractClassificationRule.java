package com.sentinelai.sentinel.classifier.rules;

import com.sentinelai.sentinel.classifier.ClassificationRule;
import com.sentinelai.sentinel.domain.RawSignalEntity;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@RequiredArgsConstructor
public abstract class AbstractClassificationRule implements ClassificationRule {
    private final String ruleId;

    @Override
    public String ruleId() {
        return ruleId;
    }

    protected boolean matchesPatterns(RawSignalEntity signal, Pattern... patterns) {
        if (isEmpty(signal.getMessage())) return false;
        for (Pattern p : patterns) {
            if (p.matcher(signal.getMessage()).find()) return true;
        }
        return false;
    }

    protected static String hintAsString(RawSignalEntity signal, String key) {
        Map<String, Object> hints = signal.getHints();
        if (hints == null) return null;
        return Objects.toString(hints.get(key), null);
    }
}
