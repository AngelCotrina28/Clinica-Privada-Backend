# Etapa 1: Construcción (Build) con Maven y Temurin 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos el archivo pom.xml y el código fuente
COPY pom.xml .
COPY src ./src

# Compilamos el proyecto omitiendo los tests
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Run) con Java ligero (Temurin JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el .jar generado en la etapa 1
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto 8080
EXPOSE 8080

# Comando para encender la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]