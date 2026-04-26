# ── Stage 1 : build ──────────────────────────────────────────────────────────
FROM maven:3-eclipse-temurin-17-alpine AS build

WORKDIR /build
COPY tarnished/ ./tarnished/

RUN cd tarnished && \
    mvn --batch-mode clean package -DskipTests -DskipITs

# ── Stage 2 : runtime ────────────────────────────────────────────────────────
FROM jetty:9.4-jre17-alpine

COPY --from=build /build/tarnished/project-app/target/project-app.war \
     /var/lib/jetty/webapps/gl2526-tarnished.war

COPY tarnished/project-app/src/main/jetty/jetty-gzip.xml \
     /var/lib/jetty/etc/jetty-gzip.xml

EXPOSE 8091

CMD ["java", "-Djetty.http.port=8091", \
     "-Djetty.etc.config.urls=etc/jetty.xml,etc/jetty-http.xml,etc/jetty-gzip.xml", \
     "-jar", "/usr/local/jetty/start.jar"]
