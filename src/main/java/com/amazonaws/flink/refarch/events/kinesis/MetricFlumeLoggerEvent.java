/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may
 * not use this file except in compliance with the License. A copy of the
 * License is located at
 *
 *    http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.flink.refarch.events.kinesis;

import java.util.Map;


public class MetricFlumeLoggerEvent extends Event {

    public int elapsedTime;
    public long ts;
    public String remoteAddress;
    public String ses;
    public String recVer;
    public String recId;
    public String recCat;
    public String app;

    public Evt evt;
    public Event event;

    public MetricFlumeLoggerEvent() {
    }

    @Override
    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

    public class Evt {
        public Map val;
        public String type;
    }

    public class Event {
        public Map val;
        public String type;
    }


}
