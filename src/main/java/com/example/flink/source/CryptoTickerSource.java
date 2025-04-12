package com.example.flink.source;

import com.example.flink.model.TickerData;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A custom source function that generates a stream of TickerData for demo purposes.
 */
public class CryptoTickerSource implements SourceFunction<TickerData> {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final String[] symbols = {"BTC/USD", "ETH/USD", "LTC/USD"};
    private final double[] basePrices = {65000.0, 3500.0, 150.0};
    private final Random random = new Random();
    private final long delayMs;

    public CryptoTickerSource(long delayMs) {
        this.delayMs = delayMs;
    }

    @Override
    public void run(SourceContext<TickerData> ctx) throws Exception {
        while (running.get()) {
            long currentTimestamp = Instant.now().toEpochMilli();

            for (int i = 0; i < symbols.length; i++) {
                // Simulate some price fluctuation
                double priceVariation = (random.nextDouble() - 0.5) * basePrices[i] * 0.001; // +/- 0.05% fluctuation
                double currentPrice = basePrices[i] + priceVariation;

                TickerData ticker = new TickerData(symbols[i], currentPrice, currentTimestamp);
                ctx.collect(ticker);
            }

            // Wait for the next emission
            Thread.sleep(delayMs);
        }
    }

    @Override
    public void cancel() {
        running.set(false);
    }
} 