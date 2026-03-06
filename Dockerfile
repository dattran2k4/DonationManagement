FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn
COPY mvnw .

COPY pom.xml .

RUN chmod +x ./mvnw

RUN ./mvnw dependency:go-offline

COPY src/ src/

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]