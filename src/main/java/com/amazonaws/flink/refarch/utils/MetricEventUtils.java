package com.amazonaws.flink.refarch.utils;

import com.amazonaws.flink.refarch.events.kinesis.MetricFlumeLoggerEvent;

public class MetricEventUtils {

    public static boolean isLaunchMetric(MetricFlumeLoggerEvent metricEvent) {
        String type = metricEvent.event.type;
        return "launchMetrics".equals(type);
    }
}
