# Flink Cryptocurrency Price Aggregation Demo (Kafka Source with Coinbase Producer)

This project demonstrates a simple Apache Flink application that processes a stream of cryptocurrency ticker data (Price, Symbol, Timestamp) read from Apache Kafka and calculates average prices over different time windows. The data is sourced in real-time from the Coinbase exchange via a WebSocket connection.

## Features

*   Ingests a stream of cryptocurrency prices (BTC/USD, ETH/USD, LTC/USD) from the `crypto-tickers` Apache Kafka topic.
*   **Includes a Python producer that connects to Coinbase WebSocket feed (`wss://ws-feed.exchange.coinbase.com`) to get real-time trade data.**
*   Uses Flink's DataStream API with Event Time semantics.
*   Applies tumbling windows of 10 seconds, 1 minute, 5 minutes, and 10 minutes.
*   Calculates the average price for each symbol within each window.
*   Prints the resulting average prices to the standard output (Task Manager logs).
*   **Sinks the resulting average prices to a second Kafka topic (`crypto-aggregations`) for external consumption.**
*   Dockerized for easy local deployment using Docker Compose, including Flink, Kafka, Zookeeper, the **Coinbase data producer**, **and a live aggregation viewer**.

## Prerequisites

*   [Docker](https://docs.docker.com/get-docker/)
*   [Docker Compose](https://docs.docker.com/compose/install/)
*   [Maven](https://maven.apache.org/install.html) (implicitly used by the Docker build process)
*   Java 11 (implicitly used by the Docker build process and Flink image)

## Project Structure

```
.
├── Dockerfile                # Defines the application's Docker image
├── docker-compose.yml        # Defines the Flink cluster, Kafka, Zookeeper, job submission, and data producer services
├── pom.xml                   # Maven project configuration
├── README.md                 # This file
├── crypto_producer.py      # Python script to fetch data from Coinbase and produce to Kafka
├── producer.Dockerfile       # Dockerfile for the Python producer service
├── requirements.txt          # Python dependencies for the producer
├── live_agg_viewer.py      # Python script to view live aggregations from Kafka
├── viewer.Dockerfile         # Dockerfile for the live aggregation viewer service
├── requirements_viewer.txt # Python dependencies for the viewer script
└── src/
    └── main/
        └── java/
            └── com/
                └── example/
                    └── flink/
                        ├── model/
                        │   └── TickerData.java       # POJO for ticker data
                        └── StreamingJob.java       # Main Flink application logic (reads from Kafka)
```

## How to Run

1.  **Clone the repository (or ensure you have the files created in this structure).**

2.  **Navigate to the project root directory** in your terminal.

3.  **Build and start the Flink cluster, Kafka, Zookeeper, data producer, and submit the job:**
    ```bash
    docker-compose up --build
    ```
    *   `--build`: Forces Docker Compose to rebuild the Flink application and producer images.
    *   This command will:
        *   Pull the necessary Flink, Kafka, and Zookeeper Docker images.
        *   Build your Flink application JAR inside a Docker container.
        *   Build the Python producer Docker image.
        *   Build the Python live aggregation viewer Docker image.
        *   Start Zookeeper, Kafka, a Flink JobManager, a TaskManager, **and the `crypto-producer` service.**
        *   Wait briefly for the services to initialize.
        *   The `crypto-producer` service will automatically connect to the Coinbase WebSocket feed and start sending BTC/USD, ETH/USD, and LTC/USD trade data to the `crypto-tickers` Kafka topic.
        *   Submit the compiled Flink JAR (`flink-crypto-demo.jar`) to the running Flink cluster. The job will start listening to the `crypto-tickers` Kafka topic.
        *   **Start the `live-agg-viewer` service, which connects to Kafka and listens for results on the `crypto-aggregations` topic.**

4.  **(Optional) View Producer Logs:**
    You can monitor the data being fetched and sent to Kafka by viewing the logs of the `crypto-producer` container:
    ```bash
    docker logs -f crypto-producer
    ```
    You should see log messages indicating connection success and received trades like: `INFO:root:Received match: BTC/USD,65100.50,1678886400000`.

5.  **View Flink UI:**
    Open your web browser and navigate to `http://localhost:8081`. You should see the Flink dashboard and your running job (`Flink Crypto Price Averaging (Kafka Source)`). Note that it might take a few moments after startup for the first data points to arrive from the producer and be processed.

6.  **View Live Aggregations:**
    The `live-agg-viewer` service automatically starts with `docker-compose up`. It consumes messages from the `crypto-aggregations` Kafka topic and prints them to its standard output.

    To see the live aggregation results, follow the logs of the `live-agg-viewer` container in a *separate terminal*:
    ```bash
    docker-compose logs -f live-agg-viewer
    # Or use: docker logs -f live-agg-viewer 
    ```

    You should see output like:
    ```
    live-agg-viewer    | INFO:root:Attempting to connect to Kafka Broker(s) at: kafka:9093 for viewing aggregations.
    live-agg-viewer    | INFO:kafka.conn:<BrokerConnection node_id=bootstrap-0 host=kafka:9093 <connecting> [IPv4 ('172.25.0.3', 9093)]> connecting
    live-agg-viewer    | INFO:kafka.conn:Broker version identified as 3.3.2
    live-agg-viewer    | INFO:kafka.conn:Set broker api version to (3, 3, 2)
    live-agg-viewer    | INFO:kafka.conn:<BrokerConnection node_id=bootstrap-0 host=kafka:9093 <connected> [IPv4 ('172.25.0.3', 9093)]> Connected
    live-agg-viewer    | INFO:root:Successfully connected. Listening for messages on topic 'crypto-aggregations'...
    live-agg-viewer    | --- Live Aggregation Viewer ---
    live-agg-viewer    | Listening to Kafka topic: crypto-aggregations on broker: kafka:9093
    live-agg-viewer    | Waiting for aggregation results... (Press Ctrl+C to stop)
    live-agg-viewer    | ------------------------------
    live-agg-viewer    | INFO:kafka.consumer.subscription_state:Updated partition assignment: [TopicPartition(topic='crypto-aggregations', partition=0)]
    live-agg-viewer    | -> BTC/USD [10s Avg]: 65102.75 (Window End: 2023-03-15T13:20:10Z)
    live-agg-viewer    | -> ETH/USD [10s Avg]: 3550.63 (Window End: 2023-03-15T13:20:10Z)
    live-agg-viewer    | -> LTC/USD [10s Avg]: 150.80 (Window End: 2023-03-15T13:20:10Z)
    ...
    ```

7.  **View Flink TaskManager Logs (Optional):**
    The calculated average prices will *also* be printed to the standard output of the TaskManager container (as we kept the `.print()` sink). You can check these logs in the first terminal where `docker-compose up` is running, or view them separately:
    ```bash
    # Find the TaskManager container ID (if needed)
    # docker ps

    # View the logs (replace <taskmanager_container_id>)
    # docker logs -f <taskmanager_container_id>
    ```
    You should see output similar to this (timestamps and prices will depend on the real-time data from Coinbase):
    ```
    taskmanager_1    | 4> BTC/USD  [10s Avg]: 65102.75 (Window: 2023-03-15T13:20:10Z)
    taskmanager_1    | 3> ETH/USD  [10s Avg]: 3550.63 (Window: 2023-03-15T13:20:10Z)
    taskmanager_1    | 5> LTC/USD  [10s Avg]: 150.80 (Window: 2023-03-15T13:20:10Z)
    ... (output for 1m, 5m, 10m windows will appear as those windows close based on the event timestamps)
    ```

8.  **Stop the application:**
    *   Stop the Docker Compose services in the first terminal (Ctrl+C).
    *   To remove the containers and network:
        ```bash
        docker-compose down
        ```

## Next Steps / Improvements

*   Use a schema registry (like Confluent Schema Registry) and Avro/Protobuf serialization instead of plain strings for Kafka messages for better schema evolution and type safety.
*   Implement more sophisticated aggregation logic (e.g., VWAP, OHLC).
*   Sink the results to a persistent store (e.g., database, monitoring system) instead of just printing or using a temporary Kafka topic.
*   Add error handling and monitoring (e.g., Flink metrics reporters).
*   Configure checkpointing and savepointing for fault tolerance.
*   Use a dedicated Kafka client library or tool to produce more realistic data streams. 