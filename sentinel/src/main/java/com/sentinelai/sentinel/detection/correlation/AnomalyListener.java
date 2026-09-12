package com.sentinelai.sentinel.detection.correlation;

import com.sentinelai.sentinel.detection.Anomaly;

public interface AnomalyListener {

    void onAnomaly(Anomaly anomaly);

}
