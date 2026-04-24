# syntax=docker/dockerfile:1.7

# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /src

# Cache dependencies
COPY pom.xml ./
COPY congvan-common/pom.xml congvan-common/
COPY congvan-auth/pom.xml congvan-auth/
COPY congvan-masterdata/pom.xml congvan-masterdata/
COPY congvan-inbound/pom.xml congvan-inbound/
COPY congvan-outbound/pom.xml congvan-outbound/
COPY congvan-workflow/pom.xml congvan-workflow/
COPY congvan-ocr/pom.xml congvan-ocr/
COPY congvan-signature/pom.xml congvan-signature/
COPY congvan-search/pom.xml congvan-search/
COPY congvan-audit/pom.xml congvan-audit/
COPY congvan-integration/pom.xml congvan-integration/
COPY congvan-app/pom.xml congvan-app/
RUN mvn -B -e -T 1C dependency:go-offline -DskipTests || true

COPY . .
RUN mvn -B -T 1C clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata curl \
    && cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime \
    && echo "Asia/Ho_Chi_Minh" > /etc/timezone

WORKDIR /app
COPY --from=build /src/congvan-app/target/congvan-app.jar /app/app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep '"status":"UP"' || exit 1

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
