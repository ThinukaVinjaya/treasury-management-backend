# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy everything first (because your project is nested)
COPY . .

# Find the correct pom.xml location and build
RUN PROJECT_DIR=$(find . -name "pom.xml" -type f | head -1 | xargs dirname) && \
    echo "Building from: $PROJECT_DIR" && \
    cd $PROJECT_DIR && \
    mvn clean package -DskipTests -B

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create a non-root user
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/**/target/*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
ENV PORT=8080
EXPOSE $PORT

# Use dumb-init
ENTRYPOINT ["dumb-init", "--"]

# Run the application
CMD ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-Dserver.port=${PORT}", "-jar", "app.jar"]