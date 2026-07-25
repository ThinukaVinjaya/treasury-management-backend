# ========================
# Stage 1: Build the application
# ========================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the entire project
COPY . .

# Find the correct pom.xml location and build
RUN PROJECT_DIR=$(find . -name "pom.xml" -type f | head -n 1 | xargs dirname) && \
    echo "========================================" && \
    echo "Building from directory: $PROJECT_DIR" && \
    echo "========================================" && \
    cd "$PROJECT_DIR" && \
    mvn clean package -DskipTests -B && \
    # Find the built JAR and copy it to a known location
    find . -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" | head -n 1 | xargs -I {} cp {} /app/app.jar && \
    echo "JAR copied successfully" && \
    ls -lh /app/app.jar

# ========================
# Stage 2: Runtime image
# ========================
FROM eclipse-temurin:21-jre-alpine

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/app.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Environment
ENV PORT=8080
EXPOSE 8080

# Start the application
ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-Dserver.port=${PORT}", "-jar", "app.jar"]