# Étape 1: Build avec Maven
FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app

# Copier les fichiers du projet
COPY . .

# Compiler l'application
RUN mvn clean package -DskipTests

# Étape 2: Exécuter l'application avec OpenJDK
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copier le JAR généré depuis la phase de build
COPY --from=build /app/target/*.jar app.jar

# Exposer le port de l'application (modifiable selon ton `application.properties`)
EXPOSE 8080

# Démarrer l'application
CMD ["java", "-jar", "app.jar"]
