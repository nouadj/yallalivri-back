package dz.nadjtech.yallalivri.controller;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderDisplayDTO;
import dz.nadjtech.yallalivri.dto.OrderStatus;
import dz.nadjtech.yallalivri.dto.UserRole;
import dz.nadjtech.yallalivri.security.JwtUtil;
import dz.nadjtech.yallalivri.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static dz.nadjtech.yallalivri.dto.UserRole.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    public OrderController(OrderService orderService, JwtUtil jwtUtil) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/status/{status}")
    public Flux<OrderDisplayDTO> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(required = false) Long idCourier,
            @RequestParam(required = false, defaultValue = "20") Integer distance,
            @RequestParam(required = false, defaultValue = "5") int hours) { // ✅ 5 heures max
        LocalDateTime since = LocalDateTime.now().minus(hours, ChronoUnit.HOURS);
        return orderService.getOrdersByStatusSinceWithDistance(status, since, distance, idCourier)
                .doOnNext(order -> System.out.println("🆕 Commande trouvée : " + order));
    }


    // 🔹 Récupérer les commandes d'un livreur (uniquement ADMIN ou le livreur lui-même)
    @GetMapping("/courier/{courierId}")
    public Mono<ResponseEntity<Flux<OrderDisplayDTO>>> getAllOrdersForCourier(
            @PathVariable Long courierId,
            @RequestParam(required = false) String status,
            ServerWebExchange exchange) {

        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    Long userIdFromToken = (Long) claims.get("userId");
                    UserRole roleFromToken = UserRole.valueOf((String) claims.get("role"));

                    // 🚨 Seul un ADMIN ou le livreur lui-même peut voir ses commandes
                    if (!courierId.equals(userIdFromToken) && roleFromToken != ADMIN) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Flux.empty()));
                    }

                    Flux<OrderDisplayDTO> orders = "ASSIGNED".equalsIgnoreCase(status)
                            ? orderService.getOrdersByCourierAndStatus(courierId, OrderStatus.ASSIGNED)
                            : orderService.getAllOrderByCourierId(courierId);

                    return Mono.just(ResponseEntity.ok(orders));
                });
    }

    // 🔹 Extraire userId et role du JWT


    @GetMapping("/store/{storeId}")
    public Mono<ResponseEntity<Flux<OrderDisplayDTO>>> getAllOrdersForStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) Integer hours,
            ServerWebExchange exchange) {

        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    Long userIdFromToken = (Long) claims.get("userId");
                    UserRole roleFromToken = UserRole.valueOf((String) claims.get("role"));

                    // 🚨 Seul un ADMIN ou le magasin lui-même peut voir ses commandes
                    if (!storeId.equals(userIdFromToken) && roleFromToken != ADMIN) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Flux.empty()));
                    }

                    Flux<OrderDisplayDTO> orders = (hours != null)
                            ? orderService.getRecentOrdersByStoreId(storeId, LocalDateTime.now().minusHours(hours))
                            : orderService.getAllOrderByStoreId(storeId);

                    return Mono.just(ResponseEntity.ok(orders));
                });
    }




    @GetMapping("/{id}")
    public Mono<OrderDTO> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping
    public Mono<ResponseEntity<OrderDTO>> createOrder(@RequestBody OrderDTO orderDTO, ServerWebExchange exchange) {
        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    UserRole roleFromToken = UserRole.valueOf((String) claims.get("role"));

                    // 🚨 Seul un ADMIN ou un magasin peut créer une commande
                    if (roleFromToken != ADMIN && roleFromToken != STORE) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }

                    return orderService.createOrder(orderDTO)
                            .map(ResponseEntity::ok);
                });
    }


    @PutMapping("/{id}")
    public Mono<OrderDTO> updateOrder(@PathVariable Long id, @RequestBody OrderDTO orderDTO) {
        return orderService.updateOrder(id, orderDTO);
    }

    @PatchMapping("/{id}/assign")
    public Mono<ResponseEntity<OrderDTO>> assignOrderToCourier(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates,
            ServerWebExchange exchange) {

        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    Long userId = (Long) claims.get("userId");
                    UserRole role = UserRole.valueOf((String) claims.get("role"));

                    // 🚨 Vérification des permissions : seul un ADMIN ou un COURRIER peut assigner une commande
                    if (role != ADMIN && role != COURIER) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }

                    // 🚨 Vérification que "courierId" est bien présent dans la requête
                    Long courierId = updates.containsKey("courierId")
                            ? ((Number) updates.get("courierId")).longValue()
                            : null;

                    if (courierId == null) {
                        return Mono.just(ResponseEntity.badRequest().body(null));
                    }

                    // 🚨 Si l'utilisateur est un COURRIER, il ne peut s'assigner une commande qu'à lui-même
                    if (role == COURIER && !courierId.equals(userId)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }

                    return orderService.assignOrderToCourier(id, updates)
                            .map(ResponseEntity::ok)
                            .onErrorResume(ResponseStatusException.class, e ->
                                    Mono.just(ResponseEntity.status(e.getStatus()).body(null)));
                });
    }


    @PatchMapping("/{id}/unassign")
    public Mono<OrderDTO> unassignOrderToCourier(
            @PathVariable Long id
            ) {
        return orderService.unassignOrderToCourier(id);
    }

    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates,
            ServerWebExchange exchange) {

        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    UserRole role = UserRole.valueOf((String) claims.get("role"));

                    // 🚨 Vérification des permissions : seul un ADMIN ou un COURRIER peut modifier le statut
                    if (role != ADMIN && role != COURIER) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }

                    // 🚨 Vérification que le champ "status" est présent dans la requête
                    if (!updates.containsKey("status")) {
                        return Mono.just(ResponseEntity.badRequest().body(null));
                    }

                    try {
                        OrderStatus newStatus = OrderStatus.valueOf(updates.get("status").toUpperCase());

                        return orderService.updateOrderStatus(id, newStatus)
                                .map(ResponseEntity::ok)
                                .onErrorResume(ResponseStatusException.class, e ->
                                        Mono.just(ResponseEntity.status(e.getStatus()).body(null)));
                    } catch (IllegalArgumentException e) {
                        return Mono.just(ResponseEntity.badRequest().body(null));
                    }
                });
    }


    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> deleteOrder(@PathVariable Long id, ServerWebExchange exchange) {

        return jwtUtil.getUserIdAndRoleFromJWT(exchange)
                .flatMap(claims -> {
                    UserRole role = UserRole.valueOf((String) claims.get("role"));
                    Long userId = (Long) claims.get("userId");

                    // 🚨 Seul un ADMIN ou un STORE peut supprimer une commande
                    if (role != ADMIN && role != STORE) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                    }

                    return orderService.deleteOrder(id, role, userId)
                            .then(Mono.just(ResponseEntity.noContent().build()))
                            .onErrorResume(ResponseStatusException.class, e ->
                                    Mono.just(ResponseEntity.status(e.getStatus()).build()));
                });
    }

}
