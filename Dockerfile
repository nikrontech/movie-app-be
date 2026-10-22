FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace
COPY . .

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon \
    && cp "$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" build/app.jar

FROM eclipse-temurin:25-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip ffmpeg \
    && pip3 install --break-system-packages --no-cache-dir yt-dlp \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home appuser \
    && mkdir -p /app/downloads/images /app/downloads/videos \
    && chown -R appuser:appuser /app
COPY --from=build /workspace/build/app.jar app.jar

USER appuser
EXPOSE 8080

ENV SPRING_MONGODB_URI=mongodb://admin:changeme@mongodb:27017/movieApp?authSource=admin

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
