package com.amazonaws.flink.refarch.events.es;

public class LaunchMetricDuration extends Document {

    public final String segment;
    public final long duration;

    public LaunchMetricDuration(long timestamp, String segment, long duration) {
        super(timestamp);
        this.segment = segment;
        this.duration = duration;
    }
}
