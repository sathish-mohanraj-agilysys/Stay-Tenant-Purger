# Use the official OpenJDK image from the Docker Hub
FROM openjdk:17-jdk-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/Stay-Tenant-Purger-0.0.1-SNAPSHOT.jar /app/Stay-Tenant-Purger-0.0.1-SNAPSHOT.jar
COPY mongoConnection.json /app/mongoConnection.json
COPY rGuestStaymap.yml /app/rGuestStaymap.yml

# Set the entry point for running the JAR, allowing arguments to be passed
ENTRYPOINT ["java", "-jar", "Stay-Tenant-Purger-0.0.1-SNAPSHOT.jar"]

# Default command, allowing runtime arguments
CMD ["--server.port=8080"]
