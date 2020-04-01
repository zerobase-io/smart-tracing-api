FROM adoptopenjdk:11-jdk-hotspot

COPY entrypoint.sh /
RUN chmod +x entrypoint.sh
ENTRYPOINT /entrypoint.sh

ENV JAVA_OPTS -Xms256m -Xmx256m

COPY target/classes/config.yml /
COPY target/smart-tracing-api.jar /app.jar
