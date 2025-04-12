# Use an official Flink runtime image with Java 11 as a parent image
# Make sure the Flink version matches the one in pom.xml
FROM flink:1.18.1-java11

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project file
COPY pom.xml .

# Copy the source code
COPY src ./src

# Build the application using Maven
# This will download dependencies and compile the code, creating the fat JAR
# We use the flink user provided by the base image
USER root
RUN chown -R flink:flink /app
USER flink
RUN mvn clean package -DskipTests

# The final JAR will be in the target directory
# Example: target/flink-crypto-demo-1.0-SNAPSHOT.jar
# We don't need to set an ENTRYPOINT here, as the job will be submitted via the Flink CLI
# using docker-compose.

# Expose the default Flink JobManager ports (optional, mainly for reference)
# EXPOSE 6123
# EXPOSE 8081

# Default command (optional, can be overridden)
# CMD ["/bin/bash"] 