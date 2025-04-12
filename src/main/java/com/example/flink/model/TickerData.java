package com.example.flink.model;

import java.util.Objects;

/**
 * POJO representing cryptocurrency ticker data.
 */
public class TickerData {

    private String symbol;
    private Double price;
    private Long timestamp;

    // Default constructor required by Flink POJO rules
    public TickerData() {}

    public TickerData(String symbol, Double price, Long timestamp) {
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TickerData that = (TickerData) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(price, that.price) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, price, timestamp);
    }

    @Override
    public String toString() {
        return "TickerData{" +
               "symbol='" + symbol + '\'' +
               ", price=" + price +
               ", timestamp=" + timestamp +
               '}';
    }
} 