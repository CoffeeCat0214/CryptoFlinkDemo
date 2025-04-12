import websocket
import json
import time
import datetime
import os
import threading
from kafka import KafkaProducer
from kafka.errors import KafkaError
import logging
import rel

# Configure logging
logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(levelname)s - %(message)s')

# --- Configuration ---
# Coinbase WebSocket Feed
COINBASE_WS_URL = "wss://ws-feed.exchange.coinbase.com"
# Symbols in Coinbase format
SYMBOLS_COINBASE = ["BTC-USD", "ETH-USD", "LTC-USD"] 
# We will subscribe to the 'matches' channel for trade data
CHANNELS_TO_SUBSCRIBE = ["matches"]

# Map Coinbase product_id back to desired output symbol (consistent with previous steps)
TARGET_SYMBOLS_MAP = { 
    "BTC-USD": "BTC/USD",
    "ETH-USD": "ETH/USD",
    "LTC-USD": "LTC/USD"
}

# Read Kafka broker from environment variable, default to localhost:9092
KAFKA_BROKER = os.environ.get("KAFKA_BROKERS_INTERNAL", "localhost:9092") 
KAFKA_TOPIC = "crypto-tickers"
# --- End Configuration ---

# Global Kafka Producer instance
kafka_producer = None

def create_kafka_producer():
    """Creates and returns a KafkaProducer instance."""
    producer = None
    logging.info(f"Attempting to connect to Kafka Broker(s) at: {KAFKA_BROKER}")
    try:
        producer = KafkaProducer(
            bootstrap_servers=[KAFKA_BROKER],
            retries=5,
            linger_ms=10,
            request_timeout_ms=60000,
        )
        logging.info("Kafka Producer connected successfully.")
    except Exception as e:
        logging.error(f"Error connecting to Kafka: {e}")
    return producer

def send_to_kafka(producer, topic, key, value):
    """Sends a message to a Kafka topic."""
    if not producer:
        logging.error("Kafka producer is not initialized. Cannot send message.")
        return
    try:
        future = producer.send(topic, key=key.encode('utf-8'), value=value.encode('utf-8'))
    except KafkaError as e:
        logging.error(f"Error sending message to Kafka: {e}")
    except Exception as e:
        logging.error(f"An unexpected error occurred during Kafka send: {e}")

# --- WebSocket Handlers ---
def on_message(ws, message):
    """Callback function for handling incoming WebSocket messages."""
    try:
        data = json.loads(message)
        msg_type = data.get('type')
        
        # Check if it's a trade message ('match' or 'ticker' can contain price)
        if msg_type == 'match': # 'match' corresponds to a trade execution
            product_id = data.get('product_id')
            if product_id in TARGET_SYMBOLS_MAP:
                output_symbol = TARGET_SYMBOLS_MAP[product_id]
                price = data.get('price')
                # Coinbase timestamp is ISO 8601 format, convert to ms epoch
                time_str = data.get('time')
                if price is not None and time_str:
                    try:
                        dt_obj = datetime.datetime.fromisoformat(time_str.replace('Z', '+00:00'))
                        timestamp_ms = int(dt_obj.timestamp() * 1000)
                        
                        # Format for Flink: SYMBOL,PRICE,TIMESTAMP
                        output_message = f"{output_symbol},{price},{timestamp_ms}"
                        logging.info(f"Received match: {output_message}")
                        send_to_kafka(kafka_producer, KAFKA_TOPIC, output_symbol, output_message)
                    except ValueError as ve:
                         logging.error(f"Error parsing timestamp {time_str}: {ve}")   
                else:
                    logging.warning(f"Missing price or time in match message: {data}")
        elif msg_type == 'error':
             logging.error(f"Received WebSocket error message: {data.get('message')}")
        elif msg_type == 'subscriptions':
             logging.info(f"Subscription confirmation received: {data}")
        # else: # Optional: log other message types like 'heartbeat' or 'ticker'
        #    logging.debug(f"Received non-match message: {data}")

    except json.JSONDecodeError:
        logging.error(f"Failed to decode JSON message: {message}")
    except Exception as e:
        logging.error(f"Error processing WebSocket message: {e} - Message: {message}")

def on_error(ws, error):
    """Callback function for WebSocket errors."""
    # Don't log BrokenPipeError if it's a clean shutdown
    if isinstance(error, BrokenPipeError):
        logging.info("WebSocket connection closed cleanly (BrokenPipeError suppressed).")
    elif isinstance(error, ConnectionResetError):
        logging.info("WebSocket connection reset by peer.")
    else:
        logging.error(f"WebSocket Error: {error}")

def on_close(ws, close_status_code, close_msg):
    """Callback function for WebSocket connection closure."""
    logging.info(f"WebSocket closed with status {close_status_code}: {close_msg}")

def on_open(ws):
    """Callback function when WebSocket connection is opened."""
    logging.info("WebSocket connection opened.")
    # Subscription message format for Coinbase
    subscribe_message = {
        "type": "subscribe",
        "product_ids": SYMBOLS_COINBASE,
        "channels": CHANNELS_TO_SUBSCRIBE
    }
    ws.send(json.dumps(subscribe_message))
    logging.info(f"Subscribed to products: {SYMBOLS_COINBASE} on channels: {CHANNELS_TO_SUBSCRIBE}")

# --- Main Execution ---
if __name__ == "__main__":
    startup_delay = 15 
    logging.info(f"Waiting {startup_delay} seconds for Kafka to initialize...")
    time.sleep(startup_delay)

    kafka_producer = create_kafka_producer()

    if not kafka_producer:
        logging.error("Exiting due to Kafka connection failure.")
        time.sleep(2)
        exit(1)
        
    logging.info(f"Connecting to WebSocket URL: {COINBASE_WS_URL}")

    ws_app = websocket.WebSocketApp(COINBASE_WS_URL,
                                  on_open=on_open,
                                  on_message=on_message,
                                  on_error=on_error,
                                  on_close=on_close)

    logging.info("Starting WebSocket listener...")
    # Use rel dispatcher for reconnect logic
    ws_app.run_forever(dispatcher=rel, reconnect=5)  
    rel.dispatch() 

    logging.info("WebSocket listener stopped.")
    if kafka_producer:
        kafka_producer.close()
        logging.info("Kafka producer closed.") 