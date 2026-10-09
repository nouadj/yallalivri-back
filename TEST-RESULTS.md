# Vérification du 9 octobre 2026

## Backend avec PostgreSQL sous Docker

Commande exécutée : docker compose -p yalla-livri-tests -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from backend-tests.

Résultat : BUILD SUCCESS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0. Le scénario vérifie les connexions, les rôles, la création des comptes et commandes, la localisation, la livraison, les accès refusés et la prise simultanée d’une commande (un succès, un conflit).

La base du test est PostgreSQL 16 avec les migrations Liquibase de production et la validation Hibernate. Les propriétés Maven remplacent la configuration H2 du profil de test. Les conteneurs et le réseau de test ont été supprimés après exécution.

Journal : postgres-docker-test.log (fichier local ignoré par Git).

## Application connectée

Commande exécutée : docker compose up -d --build.

Le backend et PostgreSQL persistants sont démarrés. Le script node scripts/smoke.mjs passe via HTTP sur cette API et nettoie ses comptes et commande temporaires.

## Front

Vérifications précédentes : TypeScript et exports web, Android et iOS réussis.

Vérification navigateur : front Expo servi sur http://localhost:8081, connecté à http://localhost:8080/api. Connexion ADMIN réussie, liste des utilisateurs PostgreSQL affichée, formulaire de création ouvert et fermé correctement.

Les parcours STORE et COURIER sont implémentés ; l’essai sur téléphone réel reste à faire, notamment GPS et appels téléphoniques. Les notifications sont désactivées par défaut.


## Notifications et interface — 9 octobre 2026

- Migration 004 appliquée au backend PostgreSQL local, sans suppression des données.
- Tests Docker avec PostgreSQL 16 et Liquibase : BUILD SUCCESS (1 scénario intégrant les contrôles de livraison, confidentialité de l’historique, lecture idempotente, lecture globale, tokens invalides/suppression du token, annulation et dates UTC). Journal : notifications-postgres-test.log.
- Le script HTTP scripts/smoke.mjs vérifie aussi les notifications création/assignation/expédition/livraison et leur lecture. Ses utilisateurs et sa commande temporaires sont supprimés après le test ; les alertes reçues par les livreurs démo restent dans leur historique.
- Front : TypeScript, export Hermes Android/iOS et export web. Modules Expo compatibles SDK 54, React Native aligné sur 0.81.5.
- Navigateur : compte livreur connecté, réception automatique de la notification de test, badge 1 puis 0 après « Tout marquer lu », message clair pour une commande supprimée. Compte magasin : compteurs et filtre « En cours » contrôlés. Écrans contrôlés à 390 × 844.
- Les émulateurs Yalla_Store et Yalla_Courier ont été relancés ; Metro 8082 répond et aucun crash ReactNativeJS/AndroidRuntime n’a été relevé dans le relevé logcat effectué. Ceci ne constitue pas une validation visuelle ni un test de réception d’alerte Android.
- Les notifications distantes restent désactivées. La configuration Expo/EAS/FCM et son guide se trouvent dans ../yalla-livri-mobile-ts/NOTIFICATIONS.md ; aucun compte externe, identifiant fictif ou clé Firebase n’a été créé. La réception Android d’alertes locales et la livraison distante application fermée restent à valider sur appareil.
