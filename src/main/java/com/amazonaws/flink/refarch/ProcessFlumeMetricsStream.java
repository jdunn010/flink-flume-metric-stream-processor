/*
    This project was evolved from the https://github.com/aws-samples/flink-stream-processing-refarch example.
 */

package com.amazonaws.flink.refarch;

import com.amazonaws.flink.refarch.events.EventSchema;
import com.amazonaws.flink.refarch.events.es.LaunchMetricDuration;
import com.amazonaws.flink.refarch.events.kinesis.CloudWatchLogEvent;
import com.amazonaws.flink.refarch.events.kinesis.MetricFlumeLoggerEvent;
import com.amazonaws.flink.refarch.utils.ElasticsearchJestSink;
import com.amazonaws.flink.refarch.utils.MetricEventUtils;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.connectors.kinesis.config.AWSConfigConstants;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;
import org.apache.flink.util.Collector;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


public class ProcessFlumeMetricsStream {
    protected static final Gson gson = new GsonBuilder()
            //.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .registerTypeAdapter(DateTime.class, (JsonDeserializer<DateTime>) (json, typeOfT, context) -> new DateTime(json.getAsString()))
            .create();
    private static final String DEFAULT_REGION = "us-west-1";
    private static final String ES_DEFAULT_INDEX = "metrics-dashboard";
    private static final Logger LOG = LoggerFactory.getLogger(ProcessFlumeMetricsStream.class);

    public static void main(String[] args) throws Exception {
        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        if (!parameterTool.has("noeventtime")) {
            env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        }

        Properties kinesisConsumerConfig = new Properties();
        kinesisConsumerConfig.setProperty(AWSConfigConstants.AWS_REGION, parameterTool.get("region", DEFAULT_REGION));
        kinesisConsumerConfig.setProperty(AWSConfigConstants.AWS_CREDENTIALS_PROVIDER, "AUTO");
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.SHARD_GETRECORDS_MAX, "10000");
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.SHARD_GETRECORDS_INTERVAL_MILLIS, "2000");
        kinesisConsumerConfig.setProperty(ConsumerConfigConstants.STREAM_INITIAL_POSITION, "TRIM_HORIZON");

        DataStream<CloudWatchLogEvent[]> kinesisStream = env.addSource(new FlinkKinesisConsumer<CloudWatchLogEvent[]>(
                //parameterTool.getRequired("stream"),
                "x1answers-log-metrics-stream-norcal",
                new EventSchema(),
                kinesisConsumerConfig)
        );
        DataStream<MetricFlumeLoggerEvent> launchMetricEvents = kinesisStream
                .rebalance()
                .flatMap(new Splitter())
                //.assignTimestampsAndWatermarks(new PunctuatedAssigner<Event>())
                .filter(event -> MetricFlumeLoggerEvent.class.isAssignableFrom(event.getClass()))
                .map(event -> event)
                .filter(MetricEventUtils::isLaunchMetric);
        DataStream<LaunchMetricDuration> durations = launchMetricEvents
                .map((MapFunction<MetricFlumeLoggerEvent, LaunchMetricDuration>) metricFlumeLoggerEvent ->
                        new LaunchMetricDuration(metricFlumeLoggerEvent.ts,
                                (String) metricFlumeLoggerEvent.event.val.get("segment"),
                                metricFlumeLoggerEvent.elapsedTime));

        final String indexName = parameterTool.get("es-index", ES_DEFAULT_INDEX);

        final ImmutableMap<String, String> config = ImmutableMap.<String, String>builder()
                .put("es-endpoint", parameterTool.getRequired("es-endpoint"))
                .put("region", parameterTool.get("region", DEFAULT_REGION))
                .build();

        durations.addSink(new ElasticsearchJestSink<>(config, indexName, "launch_metric"));
        LOG.info("Starting to consume events from stream {}", "x1answers-log-metrics-stream-norcal");

        env.execute();
    }

    public static class Splitter implements FlatMapFunction<CloudWatchLogEvent[], MetricFlumeLoggerEvent> {
        @Override
        public void flatMap(CloudWatchLogEvent[] logEvents, Collector<MetricFlumeLoggerEvent> out) throws Exception {
            for (CloudWatchLogEvent logEvent : logEvents) {
                String message = logEvent.message;
                int indexOfMetricToken = message.indexOf("METRIC ");
                String jsonContent = message.substring(indexOfMetricToken + 7);
                try {
                    MetricFlumeLoggerEvent metricFlumeLoggerEvent = gson.fromJson(jsonContent, MetricFlumeLoggerEvent.class);
                    out.collect(metricFlumeLoggerEvent);
                } catch (RuntimeException e) {
                    //ok to ignore unknown types
                    //throw e;
                }
            }
        }
    }
}
