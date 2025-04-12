# Stage 1: Build the application JAR using Maven
FROM maven:3.8.4-openjdk-11 AS builder

# Set the working directory
WORKDIR /app

# Copy the pom.xml file
COPY pom.xml .

# Download dependencies (this layer is cached if pom.xml doesn't change)
RUN mvn dependency:go-offline

# Copy the source code
COPY src ./src

# Package the application (compile, test, and create JAR)
# The fat JAR will be created in target/
RUN mvn package -DskipTests

# Stage 2: Create the final Flink application image
FROM flink:1.17.1-java11

# Set the working directory in the Flink image
WORKDIR /opt/flink

# Copy the fat JAR from the builder stage to the Flink plugins directory
# Flink automatically picks up JARs from /opt/flink/usrlib
COPY --from=builder /app/target/flink-crypto-demo-1.0-SNAPSHOT.jar /opt/flink/usrlib/flink-crypto-demo.jar

# Note: We don't need an explicit ENTRYPOINT or CMD here because
# the Flink base image provides the necessary scripts (jobmanager.sh, taskmanager.sh).
# The JAR will be submitted externally via docker-compose.

# Expose the JobManager REST port (optional, but good practice)
EXPOSE 8081 