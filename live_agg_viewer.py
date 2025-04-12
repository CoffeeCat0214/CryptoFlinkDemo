import os
import logging
from kafka import KafkaConsumer
from kafka.errors import KafkaError

# Configure logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s')

# --- Configuration ---
# Read Kafka broker from environment variable, default to localhost:9092 (exposed port)
KAFKA_BROKER_VIEWER = os.environ.get("KAFKA_BROKER_VIEWER", "kafka:9093") # Default to internal Docker network address
KAFKA_AGGREGATION_TOPIC = "crypto-aggregations"
CONSUMER_GROUP_ID_VIEWER = "live-agg-viewer-group" # Unique group ID for this consumer
# --- End Configuration ---

def main():
    """Connects to Kafka and prints messages from the aggregation topic."""
    consumer = None
    logging.info(f"Attempting to connect to Kafka Broker(s) at: {KAFKA_BROKER_VIEWER} for viewing aggregations.")
    try:
        consumer = KafkaConsumer(
            KAFKA_AGGREGATION_TOPIC,
            bootstrap_servers=[KAFKA_BROKER_VIEWER],
            auto_offset_reset='latest', # Start reading from the latest messages
            group_id=CONSUMER_GROUP_ID_VIEWER,
            # Other consumer config if needed
            # security_protocol='SSL', # Example for SSL
            # ssl_cafile='/path/to/ca.crt', # Example for SSL
        )
        logging.info(f"Successfully connected. Listening for messages on topic '{KAFKA_AGGREGATION_TOPIC}'...")

        print("--- Live Aggregation Viewer ---")
        print(f"Listening to Kafka topic: {KAFKA_AGGREGATION_TOPIC} on broker: {KAFKA_BROKER_VIEWER}")
        print("Waiting for aggregation results... (Press Ctrl+C to stop)")
        print("-" * 30)

        for message in consumer:
            # Messages have key, value, topic, partition, offset, timestamp, etc.
            # We are interested in the value, which is the formatted string from Flink.
            try:
                # Decode bytes to string
                aggregation_result = message.value.decode('utf-8')
                print(f"-> {aggregation_result}")
            except UnicodeDecodeError:
                logging.warning(f"Could not decode message value: {message.value}")
            except Exception as e:
                logging.error(f"Error processing message: {e}")

    except KafkaError as ke:
        logging.error(f"Kafka error during consumption: {ke}")
    except Exception as e:
        logging.error(f"An unexpected error occurred: {e}")
    finally:
        if consumer:
            logging.info("Closing Kafka consumer.")
            consumer.close()
        print("\nViewer stopped.")

if __name__ == "__main__":
    main() 