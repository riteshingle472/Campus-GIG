FROM eclipse-temurin
LABEL authors="rites"

WORKDIR /app

COPY target/CampusGIG-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080:8080

ENTRYPOINT ["java","-jar","app.jar"]