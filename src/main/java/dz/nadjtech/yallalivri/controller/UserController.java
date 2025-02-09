package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.UserDTO;
import dz.nadjtech.yallalivri.dto.UserRole;
import dz.nadjtech.yallalivri.dto.UserDTOWithPassword;
import dz.nadjtech.yallalivri.security.JwtUtil;
import dz.nadjtech.yallalivri.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static dz.nadjtech.yallalivri.dto.UserRole.ADMIN;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // 🔹 Récupérer tous les utilisateurs (ADMIN uniquement)
    @GetMapping
    public Mono<ResponseEntity<Flux<UserDTO>>> getAllUsers(ServerWebExchange exchange) {
        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    if (UserRole.valueOf((String) claims.get("role")) != ADMIN) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Flux.empty()));
                    }
                    return Mono.just(ResponseEntity.ok(userService.getAllUsers()));
                });
    }

    // 🔹 Récupérer un utilisateur par ID
    @GetMapping("/{userId}")
    public Mono<ResponseEntity<UserDTO>> getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // 🔹 Créer un nouvel utilisateur (ADMIN uniquement)
  /*  @PostMapping
    public Mono<ResponseEntity<Object>> createUser(@RequestBody UserDTOWithPassword userDTO, ServerWebExchange exchange) {
        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    if (UserRole.valueOf((String) claims.get("role")) != ADMIN) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("🚫 Seuls les administrateurs peuvent créer un utilisateur."));
                    }
                    return userService.createUser(userDTO).map(ResponseEntity::ok);
                });
    }
*/

    // 🔹 Créer un nouvel utilisateur (ADMIN uniquement)
    @PostMapping
    public Mono<ResponseEntity<Object>> createUser(@RequestBody UserDTOWithPassword userDTO, ServerWebExchange exchange) {

                    return userService.createUser(userDTO).map(ResponseEntity::ok);

    }

    // 🔹 Modifier partiellement un utilisateur (ex: email, téléphone)
    @PatchMapping("/{userId}")
    public Mono<ResponseEntity<Object>> patchUser(@PathVariable Long userId, @RequestBody Map<String, Object> updates, ServerWebExchange exchange) {
        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    Long userIdFromToken = (Long) claims.get("userId");
                    UserRole roleFromToken = UserRole.valueOf((String) claims.get("role"));

                    if (!userId.equals(userIdFromToken) && roleFromToken != ADMIN) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("❌ Vous ne pouvez modifier que votre propre profil."));
                    }

                    return userService.patchUser(userId, updates)
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.notFound().build());
                });
    }

    // 🔹 Changer le mot de passe
    @PatchMapping("/{userId}/password")
    public Mono<ResponseEntity<Object>> patchUserPassword(@PathVariable Long userId, @RequestBody Map<String, Object> updates, ServerWebExchange exchange) {
        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    Long userIdFromToken = (Long) claims.get("userId");
                    UserRole roleFromToken = UserRole.valueOf((String) claims.get("role"));

                    if (!userId.equals(userIdFromToken) && roleFromToken != ADMIN) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("❌ Vous ne pouvez modifier que votre propre mot de passe."));
                    }

                    return userService.patchUserPassword(userId, updates)
                            .then(Mono.just(ResponseEntity.ok("✅ Mot de passe mis à jour avec succès.")));
                });
    }

    // 🔹 Supprimer un utilisateur (ADMIN ou utilisateur lui-même)
    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long userId, ServerWebExchange exchange) {
        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    Long userIdFromToken = (Long) claims.get("userId");
                    UserRole roleFromToken = UserRole.valueOf((String) claims.get("role"));

                    if (!userId.equals(userIdFromToken) && roleFromToken != ADMIN) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }

                    return userService.deleteUser(userId)
                            .then(Mono.just(ResponseEntity.noContent().build()));
                });
    }

    // 🔹 Mettre à jour la localisation d'un utilisateur
    @PatchMapping("/{userId}/location")
    public Mono<ResponseEntity<UserDTO>> updateUserLocation(@PathVariable Long userId, @RequestBody Map<String, Double> location, ServerWebExchange exchange) {
        if (!location.containsKey("latitude") || !location.containsKey("longitude")) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    Long userIdFromToken = (Long) claims.get("userId");

                    if (!userId.equals(userIdFromToken)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }

                    return userService.updateUserLocation(userId, location.get("latitude"), location.get("longitude"))
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.notFound().build());
                });
    }

}
