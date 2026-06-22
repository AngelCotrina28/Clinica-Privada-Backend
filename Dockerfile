# Etapa 1: build con Maven y Temurin 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -B -DskipTests clean package

# Etapa 2: runtime con JRE ligero
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
