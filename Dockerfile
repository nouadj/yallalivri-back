# Étape 1: Build avec Maven
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app

# Copier d'abord le fichier POM pour éviter de reconstruire les dépendances à chaque changement de code
COPY pom.xml .
RUN mvn dependency:go-offline

# Copier le reste du projet
COPY src ./src

# Compiler le projet
RUN mvn clean package -DskipTests

# Étape 2: Exécution avec OpenJDK
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copier le fichier JAR généré depuis l’étape précédente
COPY --from=build /app/target/*.jar app.jar

# Exposer le port de l'application
EXPOSE 8080

# Lancer l'application
CMD ["java", "-jar", "app.jar"]
