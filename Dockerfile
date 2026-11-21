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
# Sized for a 512 MB free instance: a small heap, the lightest collector, small thread stacks.
ENV TZ=Asia/Kolkata \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m"
# Unpacked so the class data cache can map each library, which it cannot do inside a nested jar.
COPY --from=api /api/app.jar /tmp/app.jar
RUN java -Djarmode=tools -jar /tmp/app.jar extract --destination /app && rm /tmp/app.jar
# Class data cache: start once at build time and record the loaded classes, so each real start skips loading them (about a third faster).
# The run stops as soon as Spring is ready, before anything reaches the database, so the values below are placeholders.
RUN SLOTFORGE_DB_URL=jdbc:postgresql://localhost:1/none SLOTFORGE_DB_PASSWORD=unused GOOGLE_CLIENT_ID=unused GOOGLE_CLIENT_SECRET=unused \
    java -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar \
      --spring.profiles.active=prod,demo,google --spring.jpa.hibernate.ddl-auto=none \
      --spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
    && test -s app.jsa
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-jar", "app.jar"]
