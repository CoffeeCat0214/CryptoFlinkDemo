package com.example.flink;

import com.example.flink.model.TickerData;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StreamingJob {

    private static final String KAFKA_BROKERS = "kafka:9093"; // Use the internal Docker network hostname
    private static final String KAFKA_TOPIC = "crypto-tickers";
    private static final String CONSUMER_GROUP_ID = "flink-crypto-consumer";

    public static void main(String[] args) throws Exception {
        // Set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Configure event time
        // Note: In Flink 1.12+, setStreamTimeCharacteristic is deprecated.
        // Event time is the default and configured via WatermarkStrategy.

        // Create Kafka source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(KAFKA_BROKERS)
                .setTopics(KAFKA_TOPIC)
                .setGroupId(CONSUMER_GROUP_ID)
                .setStartingOffsets(OffsetsInitializer.latest()) // Start consuming from the latest offset
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // Define WatermarkStrategy (using event timestamps from TickerData)
        // Allow for some out-of-orderness (e.g., 5 seconds)
        WatermarkStrategy<TickerData> watermarkStrategy = WatermarkStrategy
                .<TickerData>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((event, timestamp) -> event.getTimestamp()); // Use the timestamp from the data

        // Read from Kafka, parse, and assign timestamps/watermarks
        DataStream<TickerData> stream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source") // Watermarks assigned later
                .map(new ParseTickerData()) // Parse String -> TickerData
                .filter(data -> data != null) // Filter out potential parsing errors
                .assignTimestampsAndWatermarks(watermarkStrategy);

        // --- Define Windows and Aggregations --- 

        // Key by symbol
        DataStream<String> resultStream10s = stream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.seconds(10)))
                .aggregate(new AverageAggregator(), new FormatWindowResult("10s Avg"))
                .name("10s Average");

        DataStream<String> resultStream1m = stream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .aggregate(new AverageAggregator(), new FormatWindowResult("1m Avg"))
                .name("1m Average");
        
        DataStream<String> resultStream5m = stream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.minutes(5)))
                .aggregate(new AverageAggregator(), new FormatWindowResult("5m Avg"))
                .name("5m Average");

        DataStream<String> resultStream10m = stream
                .keyBy(TickerData::getSymbol)
                .window(TumblingEventTimeWindows.of(Time.minutes(10)))
                .aggregate(new AverageAggregator(), new FormatWindowResult("10m Avg"))
                .name("10m Average");

        // --- Print Results --- 
        resultStream10s.print().name("Print 10s Avg");
        resultStream1m.print().name("Print 1m Avg");
        resultStream5m.print().name("Print 5m Avg");
        resultStream10m.print().name("Print 10m Avg");

        // Execute the job
        env.execute("Flink Crypto Price Averaging (Kafka Source)");
    }

    /**
     * Parses the input string "SYMBOL,PRICE,TIMESTAMP" into a TickerData object.
     */
    public static class ParseTickerData implements MapFunction<String, TickerData> {
        @Override
        public TickerData map(String value) throws Exception {
            try {
                String[] parts = value.split(",");
                if (parts.length == 3) {
                    String symbol = parts[0].trim();
                    Double price = Double.parseDouble(parts[1].trim());
                    Long timestamp = Long.parseLong(parts[2].trim());
                    return new TickerData(symbol, price, timestamp);
                } else {
                    // Log or handle invalid format
                    System.err.println("Invalid input format: " + value);
                    return null;
                }
            } catch (NumberFormatException e) {
                // Log or handle parsing errors
                System.err.println("Error parsing number in input: " + value + " | Error: " + e.getMessage());
                return null;
            } catch (Exception e) {
                // Log or handle other errors
                 System.err.println("Error processing input: " + value + " | Error: " + e.getMessage());
                 return null;
            }
        }
    }

    /**
     * AggregateFunction to calculate the sum and count for averaging.
     */
    public static class AverageAggregator implements AggregateFunction<TickerData, Tuple2<Double, Long>, Double> {
        @Override
        public Tuple2<Double, Long> createAccumulator() {
            return new Tuple2<>(0.0, 0L);
        }

        @Override
        public Tuple2<Double, Long> add(TickerData value, Tuple2<Double, Long> accumulator) {
            return new Tuple2<>(accumulator.f0 + value.getPrice(), accumulator.f1 + 1L);
        }

        @Override
        public Double getResult(Tuple2<Double, Long> accumulator) {
            if (accumulator.f1 == 0) {
                return 0.0; // Avoid division by zero
            }
            return accumulator.f0 / accumulator.f1;
        }

        @Override
        public Tuple2<Double, Long> merge(Tuple2<Double, Long> a, Tuple2<Double, Long> b) {
            return new Tuple2<>(a.f0 + b.f0, a.f1 + b.f1);
        }
    }

    // Simple helper class for the accumulator in AverageAggregator
    // Flink requires POJOs or Tuples for accumulators.
    public static class Tuple2<T1, T2> {
        public T1 f0;
        public T2 f1;

        public Tuple2() {}

        public Tuple2(T1 f0, T2 f1) {
            this.f0 = f0;
            this.f1 = f1;
        }
    }

    /**
     * ProcessWindowFunction to format the output string with window information.
     */
    public static class FormatWindowResult extends ProcessWindowFunction<Double, String, String, TimeWindow> {
        private final String windowTypeLabel;
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));

        public FormatWindowResult(String windowTypeLabel) {
            this.windowTypeLabel = windowTypeLabel;
        }

        @Override
        public void process(String key, Context context, Iterable<Double> elements, Collector<String> out) {
            Double average = elements.iterator().next(); // Should only be one element from AggregateFunction
            long windowEnd = context.window().getEnd();
            String formattedTime = FORMATTER.format(Instant.ofEpochMilli(windowEnd));

            out.collect(String.format("%s [%s]: %.2f (Window End: %s)",
                    key, windowTypeLabel, average, formattedTime));
        }
    }
} 