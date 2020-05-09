FROM adoptopenjdk:11-jdk-hotspot

COPY entrypoint.sh /
RUN chmod +x entrypoint.sh
ENTRYPOINT /entrypoint.sh

ENV JAVA_OPTS -Xms256m -Xmx256m

COPY api/target/api.jar /app.jar
COPY api/target/classes/config.yml /
