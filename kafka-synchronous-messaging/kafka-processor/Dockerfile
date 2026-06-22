FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /build
COPY . .
RUN mvn package -pl kafka-processor -am -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /build/kafka-processor/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
