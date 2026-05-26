# Etapa 1: Construcción (Build) con Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copiamos el archivo pom.xml y el código fuente
COPY pom.xml .
COPY src ./src

# Compilamos el proyecto omitiendo los tests para que sea más rápido
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Run) con Java ligero
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copiamos el .jar generado en la etapa 1
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto 8080 (el estándar de Spring Boot)
EXPOSE 8080

# Comando para encender la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]