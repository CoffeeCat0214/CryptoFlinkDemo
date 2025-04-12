# Use an official Python runtime as a parent image
FROM python:3.9-slim

# Set the working directory in the container
WORKDIR /app

# Copy the requirements file into the container at /app
COPY requirements_viewer.txt .

# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements_viewer.txt

# Copy the viewer script into the container at /app
COPY live_agg_viewer.py .

# Make port 9092 available to the world outside this container (optional, Kafka connection is internal)
# EXPOSE 9092 

# Define environment variable (can be overridden in docker-compose)
ENV KAFKA_BROKER_VIEWER=kafka:9093

# Run live_agg_viewer.py when the container launches
CMD ["sh", "-c", "echo 'Waiting 15 seconds for Kafka to start...' && sleep 15 && python live_agg_viewer.py"] 