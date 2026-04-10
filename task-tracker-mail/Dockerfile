FROM maven:3.9.12-eclipse-temurin-21-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and source files to the container
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

# Build the application using Maven
RUN mvn clean package -DskipTests

# === Stage 2: Run the application ===
# Use a minimal JRE image as the base for running the app
FROM eclipse-temurin:21-jre-alpine

# Set the working directory in the final image
WORKDIR /app

# Copy the built JAR file from the 'build' stage to the current stage
COPY --from=build --chown=1001:1001 /app/target/*.jar mail.jar

RUN addgroup -S appgroup && \
    adduser -S -u 1001 -G appgroup appuser

USER 1001

# Set the command to run the application when the container starts
CMD ["java", "-jar", "mail.jar"]