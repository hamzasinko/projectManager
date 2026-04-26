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

# ✅ Active le module gzip natif de Jetty (supprime l'ancien COPY et CMD)
RUN java -jar /usr/local/jetty/start.jar --add-to-start=gzip

EXPOSE 8091

CMD ["java", "-Djetty.http.port=8091", "-jar", "/usr/local/jetty/start.jar"]