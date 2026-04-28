# ── FASE 1: BUILD ────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build


WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar la caché de Docker
# Si el pom.xml no cambia, Docker no vuelve a descargar las dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el código fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# Imagen más ligera solo con Java para ejecutar
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos solo el JAR generado en la fase anterior
COPY --from=build /app/target/*.jar app.jar

# Puerto que expone el contenedor
EXPOSE 8081

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]