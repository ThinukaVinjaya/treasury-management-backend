FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app
COPY . .

# Find and build the correct pom.xml location
RUN find . -name "pom.xml" -exec dirname {} \; | head -1 > /tmp/project_path.txt
RUN PROJECT_PATH=$(cat /tmp/project_path.txt) && \
    cd $PROJECT_PATH && \
    chmod +x mvnw && \
    ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/**/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
