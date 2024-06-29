FROM openjdk:23-slim

EXPOSE 8080

COPY target/email-messages-consumer-0.0.1-SNAPSHOT.jar email-messages-consumer-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/email-messages-consumer-0.0.1-SNAPSHOT.jar"]
