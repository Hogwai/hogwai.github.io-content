FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /build
COPY . .
RUN mvn package -pl kafka-sender -am -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /build/kafka-sender/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
