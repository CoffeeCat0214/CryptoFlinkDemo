# Use an official Python runtime as a parent image
FROM python:3.9-slim

# Set the working directory in the container
WORKDIR /producer

# Install system dependencies that might be needed for some libraries (e.g., gcc for kafka-python potentially)
# RUN apt-get update && apt-get install -y --no-install-recommends gcc && rm -rf /var/lib/apt/lists/*
# Note: Above RUN commented out as it's often not needed for kafka-python on slim, but uncomment if build fails.

# Copy the requirements file into the container at /producer
COPY requirements.txt .

# Install any needed packages specified in requirements.txt
# Use --no-cache-dir to reduce image size
RUN pip install --no-cache-dir -r requirements.txt

# Copy the producer script into the container at /producer
COPY crypto_producer.py .

# Run crypto_producer.py when the container launches
CMD ["python", "./crypto_producer.py"] 