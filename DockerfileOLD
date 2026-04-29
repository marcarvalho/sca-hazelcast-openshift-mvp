FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/sca-service.jar app.jar

EXPOSE 8080
EXPOSE 5701

ENTRYPOINT ["java", "-jar", "app.jar"]
