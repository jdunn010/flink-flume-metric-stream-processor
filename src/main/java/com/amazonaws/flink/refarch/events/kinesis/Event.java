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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import org.joda.time.DateTime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;


public abstract class Event {
    protected static final Gson gson = new GsonBuilder()
            //.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .registerTypeAdapter(DateTime.class, (JsonDeserializer<DateTime>) (json, typeOfT, context) -> new DateTime(json.getAsString()))
            .create();
    private static final String TYPE_FIELD = "type";
    private static AtomicLong counter = new AtomicLong();

    public static CloudWatchLogEvent[] parseEvent(byte[] event) {
        String eventContentsString = "";
        try {
            //System.out.println("eventContentsString = " + )); ;
            //parse the event payload and remove the type attribute
            //byte[] decodeBytes = Base64.getDecoder().decode(decompress(event));
            //byte[] decompressedBytes = decompress(decodeBytes);
            //eventContentsString = new String(decompress(event));

            //int indexOfMetricToken = eventContentsString.indexOf("METRIC\n");
            InputStreamReader eventStreamReader = new InputStreamReader(new ByteArrayInputStream(decompress(event)));
            JsonReader jsonReader = new JsonReader(eventStreamReader);

            JsonElement jsonElement = Streams.parse(jsonReader);
            JsonElement logEventsArray = jsonElement.getAsJsonObject().remove("logEvents");
            CloudWatchLogEvent[] events = gson.fromJson(logEventsArray, CloudWatchLogEvent[].class);
            return events;

        } catch (Exception e) {
            //e.printStackTrace();
            throw new RuntimeException("incoming event " + eventContentsString, e);
        }
    }

    private static byte[] decompress(byte[] compressed) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(bis);
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int len;
        while ((len = gis.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }

        gis.close();
        out.close();
        return out.toByteArray();
    }


    public abstract long getTimestamp();
}