FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml lombok.config ./
COPY services ./services
RUN mvn -B -ntp -DskipTests package && mkdir /bundle &&     for service in services/*; do       jar="$service/target/$(basename "$service")-0.1.0-SNAPSHOT-exec.jar";       if test -f "$jar"; then cp "$jar" "/bundle/$(basename "$service").jar"; fi;     done
FROM eclipse-temurin:21-jre
# This image runs Java directly and does not use Ubuntu Pebble service supervision.
RUN rm -f /usr/bin/pebble
WORKDIR /app
COPY --from=build /bundle/ ./
ENV JAVA_TOOL_OPTIONS="-Xmx256m -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
ENTRYPOINT ["sh", "-c", "exec java -jar /app/${CATFI_SERVICE}.jar"]
