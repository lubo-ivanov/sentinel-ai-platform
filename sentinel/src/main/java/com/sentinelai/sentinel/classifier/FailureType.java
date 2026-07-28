package com.sentinelai.sentinel.classifier;

public enum FailureType {
    PAYMENT_PROVIDER_TIMEOUT,
    PAYMENT_DECLINED,
    PAYMENT_RETRY_EXHAUSTED,
    UNCLASSIFIED,
    INGESTION_FAILED
}
