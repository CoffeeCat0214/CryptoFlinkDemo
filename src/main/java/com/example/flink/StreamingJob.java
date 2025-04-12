package com.example.flink;

import com.example.flink.model.TickerData;
import com.example.flink.source.CryptoTickerSource;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class StreamingJob {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingJob.class);

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configure event time
        // env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime); // Deprecated in newer Flink versions

        // Add the custom source, generating data every 500ms
        DataStream<TickerData> cryptoStream = env.addSource(new CryptoTickerSource(500))
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<TickerData>forBoundedOutOfOrderness(Duration.ofSeconds(2)) // Allow 2 seconds lateness
                                .withTimestampAssigner((event, timestamp) -> event.getTimestamp()) // Use event timestamp
                );

        // --- Define Windowed Aggregations ---

        // 10 Second Window
        DataStream<String> avgPrice10s = cryptoStream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
                .aggregate(new AverageAggregator())
                .map(avg -> String.format("%-8s [10s Avg]: %.2f (Window: %s)", avg.symbol, avg.price, Instant.ofEpochMilli(avg.timestamp)));

        // 1 Minute Window
        DataStream<String> avgPrice1m = cryptoStream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .aggregate(new AverageAggregator())
                .map(avg -> String.format("%-8s [ 1m Avg]: %.2f (Window: %s)", avg.symbol, avg.price, Instant.ofEpochMilli(avg.timestamp)));

        // 5 Minute Window
        DataStream<String> avgPrice5m = cryptoStream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.minutes(5)))
                .aggregate(new AverageAggregator())
                .map(avg -> String.format("%-8s [ 5m Avg]: %.2f (Window: %s)", avg.symbol, avg.price, Instant.ofEpochMilli(avg.timestamp)));

        // 10 Minute Window
        DataStream<String> avgPrice10m = cryptoStream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.minutes(10)))
                .aggregate(new AverageAggregator())
                .map(avg -> String.format("%-8s [10m Avg]: %.2f (Window: %s)", avg.symbol, avg.price, Instant.ofEpochMilli(avg.timestamp)));


        // --- Print Results to Console (Sink) ---
        // In a real application, you would sink this to Kafka, a database, etc.
        avgPrice10s.print().name("10s Average Price Sink");
        avgPrice1m.print().name("1m Average Price Sink");
        avgPrice5m.print().name("5m Average Price Sink");
        avgPrice10m.print().name("10m Average Price Sink");

        LOG.info("Starting Flink Crypto Processing Job");
        // Execute the job
        env.execute("Flink Crypto Price Averaging");
    }

    /**
     * Aggregate function to compute the average price.
     * Accumulator: (sum, count, symbol, windowEndTimestamp)
     * Output: TickerData representing the average (symbol, average_price, windowEndTimestamp)
     */
    public static class AverageAggregator implements AggregateFunction<TickerData, Tuple4<Double, Integer, String, Long>, TickerData> {

        @Override
        public Tuple4<Double, Integer, String, Long> createAccumulator() {
            return new Tuple4<>(0.0, 0, "", 0L);
        }

        @Override
        public Tuple4<Double, Integer, String, Long> add(TickerData value, Tuple4<Double, Integer, String, Long> accumulator) {
            // Store symbol only once (it's the same for the whole key/window)
            String symbol = accumulator.f2.isEmpty() ? value.getSymbol() : accumulator.f2;
            // Update window end timestamp (will take the last one, roughly)
            long timestamp = Math.max(accumulator.f3, value.getTimestamp());
            return new Tuple4<>(accumulator.f0 + value.getPrice(), accumulator.f1 + 1, symbol, timestamp);
        }

        @Override
        public TickerData getResult(Tuple4<Double, Integer, String, Long> accumulator) {
            if (accumulator.f1 == 0) {
                return new TickerData(accumulator.f2, 0.0, accumulator.f3); // Avoid division by zero
            }
            double avgPrice = accumulator.f0 / accumulator.f1;
            // Return the window-end timestamp as the result's timestamp
            return new TickerData(accumulator.f2, avgPrice, accumulator.f3);
        }

        @Override
        public Tuple4<Double, Integer, String, Long> merge(Tuple4<Double, Integer, String, Long> a, Tuple4<Double, Integer, String, Long> b) {
            return new Tuple4<>(a.f0 + b.f0, a.f1 + b.f1, a.f2, Math.max(a.f3, b.f3)); // Symbol should be the same
        }
    }

    // Simple Tuple4 class if not using Flink's Tuple or for clarity
    public static class Tuple4<T0, T1, T2, T3> {
        public T0 f0;
        public T1 f1;
        public T2 f2;
        public T3 f3;

        public Tuple4(T0 f0, T1 f1, T2 f2, T3 f3) {
            this.f0 = f0;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
        }
    }
} 