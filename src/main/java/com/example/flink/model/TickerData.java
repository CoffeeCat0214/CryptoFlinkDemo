package com.example.flink.model;

import java.time.Instant;

public class TickerData {
    public String symbol; // e.g., BTC/USD
    public double price;
    public long timestamp; // Unix timestamp in milliseconds

    // Default constructor for Flink POJO detection
    public TickerData() {}

    public TickerData(String symbol, double price, long timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TickerData{" +
               "symbol='" + symbol + '\'' +
               ", price=" + price +
               ", timestamp=" + Instant.ofEpochMilli(timestamp) +
               '}';
    }
} 