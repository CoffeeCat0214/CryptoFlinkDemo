# Flink Cryptocurrency Price Aggregation Demo (Kafka Source)

This project demonstrates a simple Apache Flink application that processes a stream of cryptocurrency ticker data (Price, Symbol, Timestamp) read from Apache Kafka and calculates average prices over different time windows.

## Features

*   Ingests a stream of cryptocurrency prices (BTC/USD, ETH/USD, LTC/USD) from an Apache Kafka topic (`crypto-tickers`).
*   Uses Flink's DataStream API with Event Time semantics.
*   Applies tumbling windows of 10 seconds, 1 minute, 5 minutes, and 10 minutes.
*   Calculates the average price for each symbol within each window.
*   Prints the resulting average prices to the standard output (Task Manager logs).
*   Dockerized for easy local deployment using Docker Compose, including Flink, Kafka, and Zookeeper.

## Prerequisites

*   [Docker](https://docs.docker.com/get-docker/)
*   [Docker Compose](https://docs.docker.com/compose/install/)
*   [Maven](https://maven.apache.org/install.html) (implicitly used by the Docker build process)
*   Java 11 (implicitly used by the Docker build process and Flink image)

## Project Structure

```
.
├── Dockerfile                # Defines the application's Docker image
├── docker-compose.yml        # Defines the Flink cluster, Kafka, Zookeeper, and job submission service
├── pom.xml                   # Maven project configuration
├── README.md                 # This file
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

3.  **Build and start the Flink cluster, Kafka, Zookeeper, and submit the job:**
    ```bash
    docker-compose up --build
    ```
    *   `--build`: Forces Docker Compose to rebuild the Flink application image.
    *   This command will:
        *   Pull the necessary Flink, Kafka, and Zookeeper Docker images.
        *   Build your Flink application JAR inside a Docker container.
        *   Start Zookeeper, Kafka, a Flink JobManager, and a TaskManager.
        *   Wait briefly for the services to initialize.
        *   Submit the compiled JAR (`flink-crypto-demo-1.0-SNAPSHOT.jar`) to the running Flink cluster. The job will start listening to the `crypto-tickers` Kafka topic.

4.  **Produce Data to Kafka:**
    While the `docker-compose up` command is running in one terminal, open a *second terminal* and run the following command to start the Kafka console producer. This allows you to send messages to the `crypto-tickers` topic.
    ```bash
    # Find the Kafka container ID
    docker ps

    # Run the console producer (replace <kafka_container_id>)
    docker exec -it <kafka_container_id> kafka-console-producer.sh --broker-list kafka:9093 --topic crypto-tickers
    ```
    Once the producer prompt (`>`) appears, you can type messages in the format `SYMBOL,PRICE,TIMESTAMP` (where TIMESTAMP is the epoch milliseconds) and press Enter. Each line is a message sent to Kafka.

    **Example Input:**
    ```
    > BTC/USD,65100.50,1678886400000
    > ETH/USD,3550.25,1678886401000
    > BTC/USD,65105.00,1678886402000
    > LTC/USD,150.80,1678886403000
    > ETH/USD,3551.00,1678886404000
    ```
    *You can generate current epoch milliseconds using online tools or commands like `date +%s%3N` on Linux/macOS.* Send a few messages for different symbols.

5.  **View Flink UI:**
    Open your web browser and navigate to `http://localhost:8081`. You should see the Flink dashboard and your running job (`Flink Crypto Price Averaging (Kafka Source)`).

6.  **View Output:**
    The calculated average prices will be printed to the standard output of the TaskManager container. Check the logs in the first terminal where `docker-compose up` is running, or view them separately:
    ```bash
    # Find the TaskManager container ID (if needed)
    # docker ps

    # View the logs (replace <taskmanager_container_id>)
    # docker logs -f <taskmanager_container_id>
    ```
    You should see output similar to this (timestamps and prices will depend on your input):
    ```
    taskmanager_1    | 4> BTC/USD  [10s Avg]: 65102.75 (Window: 2023-03-15T13:20:10Z) 
    taskmanager_1    | 3> ETH/USD  [10s Avg]: 3550.63 (Window: 2023-03-15T13:20:10Z)
    taskmanager_1    | 5> LTC/USD  [10s Avg]: 150.80 (Window: 2023-03-15T13:20:10Z)
    ... (output for 1m, 5m, 10m windows will appear as those windows close based on the event timestamps)
    ```

7.  **Stop the application:**
    *   Stop the Kafka producer in the second terminal (Ctrl+C).
    *   Stop the Docker Compose services in the first terminal (Ctrl+C).
    *   To remove the containers and network:
        ```bash
        docker-compose down
        ```

## Next Steps / Improvements

*   Use a schema registry (like Confluent Schema Registry) and Avro/Protobuf serialization instead of plain strings for Kafka messages for better schema evolution and type safety.
*   Implement more sophisticated aggregation logic (e.g., VWAP, OHLC).
*   Sink the results to a persistent store (e.g., database, another Kafka topic, monitoring system) instead of just printing.
*   Add error handling and monitoring (e.g., Flink metrics reporters).
*   Configure checkpointing and savepointing for fault tolerance.
*   Use a dedicated Kafka client library or tool to produce more realistic data streams. 