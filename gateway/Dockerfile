FROM openjdk:23-slim

EXPOSE 8080
EXPOSE 5005

COPY target/gateway-0.0.1-SNAPSHOT.jar gateway-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","-jar","/gateway-0.0.1-SNAPSHOT.jar"]
