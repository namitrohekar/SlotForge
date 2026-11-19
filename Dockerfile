# One image: the React app is built, then served by Spring Boot next to the API, so both share one address.

FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS api
WORKDIR /api
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY backend/src src
COPY --from=web /web/dist/ src/main/resources/static/
RUN ./mvnw -q -B -DskipTests package && cp target/slotforge-*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=api /api/app.jar app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
# Sized for a 512 MB free instance: a small heap, the lightest collector, small thread stacks.
ENV TZ=Asia/Kolkata \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
