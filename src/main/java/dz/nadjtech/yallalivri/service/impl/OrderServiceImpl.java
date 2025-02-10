package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.*;
import dz.nadjtech.yallalivri.entity.Order;
import dz.nadjtech.yallalivri.entity.User;
import dz.nadjtech.yallalivri.mapper.OrderMapper;
import dz.nadjtech.yallalivri.repository.OrderRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.OrderService;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.NoSuchElementException;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Flux<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .map(OrderMapper::toDTO);
    }

    @Override
    public Mono<OrderDTO> getOrderById(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found")))
                .map(OrderMapper::toDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<OrderDTO> createOrder(OrderDTO orderDTO) {
        // On peut vérifier la validité du Store ici, par ex.:
        return userRepository.findById(orderDTO.getStoreId())
                .switchIfEmpty(Mono.error(new NoSuchElementException("Store not found")))
                .flatMap(store -> {
                    Order entity = OrderMapper.toEntity(orderDTO);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(entity);
                })
                .map(OrderMapper::toDTO)
                .doOnSuccess(this::notifyCouriers); // notifie en asynchrone
    }

    /**
     * Notifie les coursiers à proximité si le store a des coordonnées.
     */
    private void notifyCouriers(OrderDTO order) {
        userRepository.findById(order.getStoreId())
                .flatMapMany(store -> {
                    if (store.getLatitude() == null || store.getLongitude() == null) {
                        return Flux.empty(); // pas de notif possible
                    }
                    final double maxDistanceKm = 20.0;
                    final Point storeLocation = new Point(store.getLatitude(), store.getLongitude());

                    return userRepository.findAllByRole(UserRole.COURIER)
                            .filter(c -> c.getNotificationToken() != null && c.getLatitude() != null && c.getLongitude() != null)
                            .filter(c -> {
                                Point courierLoc = new Point(c.getLatitude(), c.getLongitude());
                                double distance = calculateDistance(storeLocation, courierLoc);
                                return distance <= maxDistanceKm;
                            })
                            .doOnNext(courier -> sendPushNotificationToCourier(courier.getNotificationToken(), store.getName(), order));
                })
                .subscribe(); // On déclenche l'exécution (asynchrone)
    }

    private double calculateDistance(Point p1, Point p2) {
        double earthRadius = 6371.0; // km
        double latDiff = Math.toRadians(p2.getX() - p1.getX());
        double lonDiff = Math.toRadians(p2.getY() - p1.getY());
        double a = Math.sin(latDiff/2) * Math.sin(latDiff/2)
                + Math.cos(Math.toRadians(p1.getX())) * Math.cos(Math.toRadians(p2.getX()))
                * Math.sin(lonDiff/2) * Math.sin(lonDiff/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private void sendPushNotificationToCourier(String expoToken, String storeName, OrderDTO order) {
        // Ex : WebClient pour Expo
        WebClient.create("https://exp.host")
                .post()
                .uri("/--/api/v2/push/send")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Map.of(
                        "to", expoToken,
                        "title", storeName + " 📦 Nouvelle commande dispo !",
                        "body", "Commande de " + order.getCustomerName(),
                        "data", Map.of("orderId", order.getId())
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> System.out.println("✅ Notification envoyée : " + response),
                        err -> System.err.println("❌ Erreur notif : " + err.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<OrderDTO> updateOrder(Long id, OrderDTO orderDTO) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found")))
                .flatMap(existing -> {
                    existing.setStoreId(orderDTO.getStoreId());
                    existing.setCourierId(orderDTO.getCourierId());
                    existing.setCustomerName(orderDTO.getCustomerName());
                    existing.setCustomerPhone(orderDTO.getCustomerPhone());
                    existing.setCustomerAddress(orderDTO.getCustomerAddress());
                    existing.setStatus(orderDTO.getStatus());
                    existing.setAmount(orderDTO.getAmount());
                    existing.setDeliveryFee(orderDTO.getDeliveryFee());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(existing);
                })
                .map(OrderMapper::toDTO);
    }

    /**
     * Notifie le store d’un événement (commande assignée, livrée, etc.)
     */
    private void notifyStore(OrderDTO order, String message) {
        userRepository.findById(order.getStoreId())
                .flatMap(store -> {
                    if (store.getNotificationToken() == null) {
                        // Pas de token => pas de notif
                        return Mono.empty();
                    }
                    // Envoie la notification
                    return sendPushNotificationStore(
                            store.getNotificationToken(),
                            "Commande " + order.getId() + " " + message,
                            "La commande pour " + order.getCustomerName() + " est " + message + ".",
                            order.getId()
                    );
                })
                .subscribe(); // On déclenche l'appel asynchrone
    }
    /**
     * Envoie réellement la notif push via Expo
     */
    private Mono<Void> sendPushNotificationStore(String expoToken, String title, String body, Long orderId) {
        return WebClient.create("https://exp.host")
                .post()
                .uri("/--/api/v2/push/send")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Map.of(
                        "to", expoToken,
                        "title", title,
                        "body", body,
                        "data", Map.of("orderId", orderId)
                ))
                .retrieve()
                .bodyToMono(String.class)
                // On retourne Mono<Void> après la réponse
                .then();
    }


    @Override
    public Mono<OrderDTO> updateOrderStatus(Long id, OrderStatus newStatus) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found")))
                .flatMap(existingOrder -> {
                    if (!isValidTransition(existingOrder.getStatus(), newStatus)) {
                        return Mono.error(new IllegalArgumentException("Invalid status transition"));
                    }
                    existingOrder.setStatus(newStatus);
                    existingOrder.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(existingOrder);
                })
                .map(OrderMapper::toDTO)
                // 🔥 Notifier le store si c'est DELIVERED ou RETURNED
                .doOnSuccess(orderDto -> {
                    if (newStatus == OrderStatus.DELIVERED) {
                        notifyStore(orderDto, "livrée");
                    } else if (newStatus == OrderStatus.RETURNED) {
                        notifyStore(orderDto, "retournée");
                    }
                });
    }


    private boolean isValidTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        return switch (oldStatus) {
            case CREATED -> (newStatus == OrderStatus.ASSIGNED || newStatus == OrderStatus.CANCELLED);
            case ASSIGNED -> (newStatus == OrderStatus.RETURNED || newStatus == OrderStatus.DELIVERED);
            default -> false;
        };
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<Void> deleteOrder(Long id, UserRole role, Long userId) {
        if (role == UserRole.ADMIN) {
            return orderRepository.deleteById(id);
        }
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found")))
                .flatMap(order -> {
                    // Seul le store ayant créé la commande peut la supprimer
                    if (!Objects.equals(order.getStoreId(), userId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied"));
                    }
                    // Si CREATED ou RETURNED -> peut supprimer
                    if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.RETURNED) {
                        return orderRepository.delete(order);
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Can't delete this order"));
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // FIND BY COURIER / STORE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Flux<OrderDisplayDTO> getAllOrderByCourierId(Long courierId) {
        return orderRepository.findByCourierIdOrderByUpdatedAtDesc(courierId)
                .flatMap(this::enrichOrder);
    }

    @Override
    public Flux<OrderDisplayDTO> getAllOrderByStoreId(Long storeId) {
        return orderRepository.findByStoreIdOrderByUpdatedAtDesc(storeId)
                .flatMap(this::enrichOrder);
    }

    @Override
    public Flux<OrderDisplayDTO> getRecentOrdersByStoreId(Long storeId, LocalDateTime since) {
        return orderRepository.findByStoreIdAndCreatedAtAfterOrderByUpdatedAtDesc(storeId, since)
                .flatMap(this::enrichOrder);
    }

    @Override
    public Flux<OrderDisplayDTO> getOrdersByCourierAndStatus(Long courierId, OrderStatus orderStatus) {
        return orderRepository.findByCourierIdAndStatus(courierId, orderStatus)
                .flatMap(this::enrichOrder);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // RECHERCHE PAR STATUS + DISTANCE + SINCE
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Flux<OrderDisplayDTO> getOrdersByStatusSinceWithDistance(String status, LocalDateTime since, Integer maxDistanceKm, Long courierId) {
        return userRepository.findById(courierId)
                .flatMapMany(courier -> {
                    if (courier.getLatitude() == null || courier.getLongitude() == null) {
                        return Flux.empty(); // pas de position => pas de résultats
                    }
                    final Point courierLoc = new Point(courier.getLatitude(), courier.getLongitude());
                    return orderRepository.findByStatusAndCreatedAtAfterOrderByUpdatedAtDesc(status, since)
                            .flatMap(order ->
                                    userRepository.findById(order.getStoreId())
                                            .flatMap(store -> {
                                                if (store.getLatitude() == null || store.getLongitude() == null) {
                                                    return Mono.empty();
                                                }
                                                double dist = calculateDistance(
                                                        new Point(store.getLatitude(), store.getLongitude()),
                                                        courierLoc
                                                );
                                                return dist <= maxDistanceKm ? enrichOrder(order) : Mono.empty();
                                            })
                            );
                });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ASSIGN / UNASSIGN
    // ─────────────────────────────────────────────────────────────────────────────
    @Override
    public Mono<OrderDTO> assignOrderToCourier(Long id, Map<String, Object> updates) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found")))
                .flatMap(order -> {
                    if (order.getCourierId() != null) {
                        return Mono.error(new IllegalStateException("Commande déjà assignée"));
                    }
                    if (updates.containsKey("courierId")) {
                        order.setCourierId(Long.valueOf(updates.get("courierId").toString()));
                    }
                    if (updates.containsKey("status")) {
                        order.setStatus(OrderStatus.valueOf(updates.get("status").toString()));
                    }
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order);
                })
                .map(OrderMapper::toDTO)
                // 🔥 Notifier le store que la commande est "assignée"
                .doOnSuccess(orderDto -> {
                    if (orderDto.getStatus() == OrderStatus.ASSIGNED) {
                        notifyStore(orderDto, "assignée à un livreur");
                    }
                });
    }


    @Override
    public Mono<OrderDTO> unassignOrderToCourier(Long id) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found")))
                .flatMap(existingOrder -> {
                    if (existingOrder.getStatus() == OrderStatus.ASSIGNED) {
                        existingOrder.setCourierId(null);
                        existingOrder.setUpdatedAt(LocalDateTime.now());
                        return orderRepository.save(existingOrder);
                    } else {
                        return Mono.error(new IllegalArgumentException("Cannot unassign an order not in ASSIGNED status"));
                    }
                })
                .map(OrderMapper::toDTO);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ENRICH ORDER
    // ─────────────────────────────────────────────────────────────────────────────
    private Mono<OrderDisplayDTO> enrichOrder(Order order) {
        Mono<User> storeMono = userRepository.findById(order.getStoreId())
                .switchIfEmpty(Mono.error(new NoSuchElementException("Store not found for order " + order.getId())));

        Mono<User> courierMono;
        if (order.getCourierId() != null) {
            courierMono = userRepository.findById(order.getCourierId())
                    .defaultIfEmpty(new User());
        } else {
            courierMono = Mono.just(new User());
        }

        return storeMono.zipWith(courierMono)
                .map(tuple -> {
                    User storeUser = tuple.getT1();
                    User courierUser = tuple.getT2();
                    String storeName = storeUser.getName();
                    String storeAddress = storeUser.getAddress();
                    String courierName = courierUser.getName() == null ? "" : courierUser.getName();
                    return OrderMapper.toDisplayDTO(order, storeName, storeAddress, courierName);
                });
    }
}
