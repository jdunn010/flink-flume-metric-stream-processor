package com.amazonaws.flink.refarch.events.kinesis;

public class CloudWatchLogEvent extends Event {
    public String id;
    public Long timestamp;
    public String message;

    @Override
    public long getTimestamp() {
        return 0;
    }
}
