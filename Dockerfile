# Étape 1: Construire l'application avec Maven
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Étape 2: Utiliser une image Java légère pour exécuter l'application
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Exposer le port (par défaut 8080 pour Spring Boot)
EXPOSE 8080

# Lancer l'application
CMD ["java", "-jar", "app.jar"]
