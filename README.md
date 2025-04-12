# Flink Cryptocurrency Price Aggregation Demo

This project demonstrates a simple Apache Flink application that processes a simulated stream of cryptocurrency ticker data (Price, Symbol, Timestamp) and calculates average prices over different time windows.

## Features

*   Ingests a simulated stream of cryptocurrency prices (BTC/USD, ETH/USD, LTC/USD).
*   Uses Flink's DataStream API with Event Time semantics.
*   Applies tumbling windows of 10 seconds, 1 minute, 5 minutes, and 10 minutes.
*   Calculates the average price for each symbol within each window.
*   Prints the resulting average prices to the standard output (Task Manager logs).
*   Dockerized for easy local deployment using Docker Compose.

## Prerequisites

*   [Docker](https://docs.docker.com/get-docker/)
*   [Docker Compose](https://docs.docker.com/compose/install/)
*   [Maven](https://maven.apache.org/install.html) (implicitly used by the Docker build process)
*   Java 11 (implicitly used by the Docker build process and Flink image)

## Project Structure

```
.
├── Dockerfile                # Defines the application's Docker image
├── docker-compose.yml        # Defines the Flink cluster and job submission service
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
                        ├── source/
                        │   └── CryptoTickerSource.java # Simulates data stream
                        └── StreamingJob.java       # Main Flink application logic
```

## How to Run

1.  **Clone the repository (or ensure you have the files created in this structure).**

2.  **Navigate to the project root directory** in your terminal.

3.  **Build and start the Flink cluster and submit the job:**
    ```bash
    docker-compose up --build
    ```
    *   `--build`: Forces Docker Compose to rebuild the application image defined in the `Dockerfile`.
    *   This command will:
        *   Pull the necessary Flink Docker images.
        *   Build your Flink application JAR inside a Docker container.
        *   Start a Flink JobManager and a TaskManager.
        *   Submit the compiled JAR (`flink-crypto-demo-1.0-SNAPSHOT.jar`) to the running Flink cluster.

4.  **View Flink UI:**
    Open your web browser and navigate to `http://localhost:8081`. You should see the Flink dashboard and your running job (`Flink Crypto Price Averaging`).

5.  **View Output:**
    The calculated average prices will be printed to the standard output of the TaskManager container. You can view these logs using:
    ```bash
    # Find the TaskManager container ID
    docker ps

    # View the logs (replace <taskmanager_container_id>)
    docker logs -f <taskmanager_container_id>
    ```
    You should see output similar to this (timestamps and prices will vary):
    ```
    taskmanager-1    | 6> ETH/USD  [10s Avg]: 3501.12 (Window: 2023-10-27T10:30:10Z)
    taskmanager-1    | 7> BTC/USD  [10s Avg]: 64985.50 (Window: 2023-10-27T10:30:10Z)
    taskmanager-1    | 8> LTC/USD  [10s Avg]: 149.95 (Window: 2023-10-27T10:30:10Z)
    taskmanager-1    | 6> ETH/USD  [10s Avg]: 3499.88 (Window: 2023-10-27T10:30:20Z)
    ... (output for 1m, 5m, 10m windows will appear as those windows close)
    ```

6.  **Stop the application:**
    Press `Ctrl + C` in the terminal where `docker-compose up` is running. To remove the containers and network:
    ```bash
    docker-compose down
    ```

## Next Steps / Improvements

*   Replace the `CryptoTickerSource` with a real data source connector (e.g., Kafka, Pulsar, Kinesis) to ingest actual live data.
*   Implement more sophisticated aggregation logic (e.g., VWAP, OHLC).
*   Sink the results to a persistent store (e.g., database, Kafka, monitoring system) instead of just printing.
*   Add error handling and monitoring.
*   Configure checkpointing and savepointing for fault tolerance. 