FROM openjdk:23-slim

EXPOSE 8080
EXPOSE 5006

COPY target/execution-facts-and-participant-api-0.0.1-SNAPSHOT.jar execution-facts-and-participant-api-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006","-jar","/execution-facts-and-participant-api-0.0.1-SNAPSHOT.jar"]
