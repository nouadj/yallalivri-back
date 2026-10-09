# Backend Yalla Livri

Java 21, Spring Boot MVC, JPA, JWT, Liquibase et PostgreSQL.

## Lancement local

Prérequis : Docker Desktop pour lancer le backend et PostgreSQL. Pour lancer Java directement : JDK 21.

Copier `.env.example` vers `.env` si absent, puis lancer :

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

L’API est disponible sur `http://localhost:8080/api`. PostgreSQL est exposé uniquement sur `127.0.0.1:5433`, avec un volume Docker persistant.

Les valeurs de `.env.example` servent aux tests locaux : elles créent le compte `admin@yallalivri.dz` / `admin1234`. Pour un déploiement, définir un secret JWT et des mots de passe propres à l’environnement, puis désactiver `BOOTSTRAP_ADMIN_ENABLED` après création de l’administrateur. `.env` est exclu de Git.

Base seule : docker compose up -d postgres. Backend Java local : .\start-local.ps1.

Tests : .\mvnw.cmd test. Test sur API démarrée : node scripts/smoke.mjs.

## API et notifications

`POST /api/auth/login` retourne le JWT à utiliser dans l’en-tête `Authorization: Bearer <token>`. L’administration gère les comptes via `/api/users` ; magasins et livreurs utilisent `/api/orders` avec des contrôles de rôle et de propriété. Le parcours de livraison suit CREATED → ASSIGNED → SHIPPED → DELIVERED ou RETURNED.

L’historique privé des notifications est disponible via `GET /api/notifications`, `PATCH /api/notifications/{id}/read` et `POST /api/notifications/read-all`. Les notifications sont enregistrées dans PostgreSQL avec la transaction de commande. La création avertit les livreurs proches ; les changements de statut avertissent le magasin.

Pour les notifications distantes, configurer Expo/Firebase dans l’application Android dédiée et définir `NOTIFICATIONS_ENABLED=true` dans l’environnement du backend. L’application enregistre son token Expo via `PATCH /api/auth/users/{id}/notification-token`. Le backend transmet les alertes à Expo après validation de la transaction. Une erreur d’envoi conserve l’historique mais n’est pas relancée automatiquement ; les reçus de livraison Expo ne sont pas encore suivis.

## Tests PostgreSQL dans Docker

Depuis ce dossier, exécuter :

```powershell
docker compose -p yalla-livri-tests -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from backend-tests
docker compose -p yalla-livri-tests -f docker-compose.test.yml down
```

La base PostgreSQL 16 de test est éphémère, sans port publié et sans volume de l’application. Le conteneur Maven exécute les tests existants sur cette vraie base, avec les migrations de production et ddl-auto=validate. Le profil H2 est remplacé par les propriétés datasource et Liquibase passées à Maven. Le code de sortie du conteneur backend-tests indique le résultat. Les tests incluent la livraison complète, les autorisations et la prise simultanée par deux livreurs.
