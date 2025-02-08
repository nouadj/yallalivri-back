package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.*;
import dz.nadjtech.yallalivri.entity.Order;
import dz.nadjtech.yallalivri.mapper.OrderMapper;
import dz.nadjtech.yallalivri.repository.OrderRepository;
import dz.nadjtech.yallalivri.repository.UserRepository;
import dz.nadjtech.yallalivri.service.CourierService;
import dz.nadjtech.yallalivri.service.OrderService;
import dz.nadjtech.yallalivri.service.StoreService;
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

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StoreService storeService;
    private final CourierService courierService;

    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository, StoreService storeService, CourierService courierService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.storeService = storeService;
        this.courierService = courierService;
    }


    @Override
    public Flux<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .map(OrderMapper::toDTO);
    }

    @Override
    public Mono<OrderDTO> getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderMapper::toDTO);
    }

    @Override
    public Mono<OrderDTO> createOrder(OrderDTO orderDTO) {
        return orderRepository.save(OrderMapper.toEntity(orderDTO))
                .map(OrderMapper::toDTO)
                .doOnSuccess(order -> notifyCouriers(order));
    }

    private void notifyCouriers(OrderDTO order) {
        // 🔥 Vérifie si les coordonnées du magasin sont disponibles
        userRepository.findById(order.getStoreId())
                .flatMap(store -> {
                    if (store == null || store.getLatitude() == null || store.getLongitude() == null) {
                        return Mono.empty(); // 🚨 Évite une erreur si les coordonnées ne sont pas disponibles
                    }
                    String storeName = store.getName();
                    Double storeLatitude = store.getLatitude();
                    Double storeLongitude = store.getLongitude();

                    Point storeLocation = new Point(storeLatitude, storeLongitude);
                    double maxDistanceKm = 20.0; // Rayon de recherche (20 km)

                    return userRepository.findAllByRole(UserRole.COURIER)
                            .filter(user -> user.getNotificationToken() != null && user.getLatitude() != null && user.getLongitude() != null)
                            .filter(courier -> {
                                Point courierLocation = new Point(courier.getLatitude(), courier.getLongitude());
                                double distance = calculateDistance(storeLocation, courierLocation);
                                return distance <= maxDistanceKm;
                            })
                            .doOnNext(courier -> sendPushNotification(courier.getNotificationToken(), storeName, order))
                            .then();
                })
                .subscribe();
    }


    private double calculateDistance(Point p1, Point p2) {
        double earthRadius = 6371.0; // Rayon de la Terre en km
        double latDiff = Math.toRadians(p2.getX() - p1.getX());
        double lonDiff = Math.toRadians(p2.getY() - p1.getY());

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(p1.getX())) * Math.cos(Math.toRadians(p2.getX()))
                * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private void sendPushNotification(String expoToken, String storeName, OrderDTO order) {
        userRepository.findById(order.getId());
        WebClient.create("https://exp.host")
                .post()
                .uri("/--/api/v2/push/send")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Map.of(
                        "to", expoToken,
                        "title", storeName + " 📦 Nouvelle commande disponible !",
                        "body", storeName + " une nouvelle commande de " + order.getCustomerName() + " est disponible.",
                        "data", Map.of("orderId", order.getId())
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> System.out.println("✅ Notification envoyée : " + response));
    }


    @Override
    public Mono<OrderDTO> updateOrder(Long id, OrderDTO orderDTO) {
        return orderRepository.findById(id)
                .flatMap(existingOrder -> {
                    existingOrder.setStoreId(orderDTO.getStoreId());
                    existingOrder.setCourierId(orderDTO.getCourierId());
                    existingOrder.setCustomerName(orderDTO.getCustomerName());
                    existingOrder.setCustomerPhone(orderDTO.getCustomerPhone());
                    existingOrder.setCustomerAddress(orderDTO.getCustomerAddress());
                    existingOrder.setStatus(orderDTO.getStatus());
                    existingOrder.setAmount(orderDTO.getAmount());
                    existingOrder.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(existingOrder);
                })
                .map(OrderMapper::toDTO);
    }

    @Override
    public Mono<OrderDTO> updateOrderStatus(Long id, OrderStatus newOrderStatus) {
        return orderRepository.findById(id)
                .flatMap(existingOrder -> {
                    if (isValidTransition(existingOrder.getStatus(), newOrderStatus)) {
                        existingOrder.setStatus(newOrderStatus);
                        existingOrder.setUpdatedAt(LocalDateTime.now());
                    } else {
                        return Mono.error(new Throwable());
                    }
                    return orderRepository.save(existingOrder);
                }).map(OrderMapper::toDTO);
    }


    private boolean isValidTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        return switch (oldStatus) {
            case CREATED ->
                // From CREATED, only allow ASSIGNED or CANCELLED
                    newStatus == OrderStatus.ASSIGNED || newStatus == OrderStatus.CANCELLED;
            case ASSIGNED ->
                // From SHIPPED, only allow RETURNED or DELIVERED
                    newStatus == OrderStatus.RETURNED || newStatus == OrderStatus.DELIVERED;
            default -> false;
        };
    }

    @Override
    public Mono<Void> deleteOrder(Long id, UserRole role, Long userId) {
        if (role == UserRole.ADMIN) {
            return orderRepository.deleteById(id);
        }

        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande non trouvée")))
                .flatMap(order -> {
                    if (Objects.equals(order.getStoreId(), userId)) {
                        if(order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.RETURNED) {
                            return orderRepository.deleteById(id);
                        } else {
                            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'avez pas le droit de supprimer cette commande"));
                        }
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'avez pas le droit de supprimer cette commande"));
                    }
                });
    }


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
    public Flux<OrderDisplayDTO> getOrdersByStatusSinceWithDistance(String status, LocalDateTime since, Integer maxDistanceKm, Long idCourier) {
        return userRepository.findById(idCourier)
                .flatMapMany(courier -> {
                    if (courier.getLatitude() == null || courier.getLongitude() == null) {
                        return Flux.empty(); // Si le livreur n'a pas de position, on ne renvoie rien
                    }

                    Point courierLocation = new Point(courier.getLatitude(), courier.getLongitude());

                    return orderRepository.findByStatusAndCreatedAtAfterOrderByUpdatedAtDesc(status, since)
                            .flatMap(order -> userRepository.findById(order.getStoreId()) // 🔥 Trouver le magasin
                                    .flatMap(store -> {
                                        if (store.getLatitude() == null || store.getLongitude() == null) {
                                            return Mono.empty(); // Si le magasin n'a pas de coordonnées, on ignore
                                        }

                                        Point storeLocation = new Point(store.getLatitude(), store.getLongitude());

                                        // 🔥 Utilisation directe de `calculateDistance()`
                                        double distance = calculateDistance(storeLocation, courierLocation);

                                        // ✅ Retourner uniquement les commandes où la distance ≤ maxDistanceKm
                                        return distance <= maxDistanceKm ? enrichOrder(order) : Mono.empty();
                                    })
                            );
                });
    }


    @Override
    public Mono<OrderDTO> assignOrderToCourier(Long id, Map<String, Object> updates) {
        return orderRepository.findById(id)
                .flatMap(order -> {
                    if (order.getCourierId() != null) {
                        return Mono.error(new IllegalStateException("Commande déjà assignée à un autre livreur"));
                    }

                    if (updates.containsKey("courierId")) {
                        order.setCourierId(Long.valueOf(updates.get("courierId").toString()));
                    }
                    if (updates.containsKey("status")) {
                        order.setStatus(OrderStatus.valueOf(updates.get("status").toString()));
                    }
                    return orderRepository.save(order);
                })
                .map(OrderMapper::toDTO);
    }


    @Override
    public Flux<OrderDisplayDTO> getOrdersByCourierAndStatus(Long courierId, OrderStatus orderStatus) {
        return this.orderRepository.findByCourierIdAndStatus(courierId, orderStatus).flatMap(this::enrichOrder);
    }

    @Override
    public Flux<OrderDisplayDTO> getRecentOrdersByStoreId(Long storeId, LocalDateTime since) {
        return orderRepository.findByStoreIdAndCreatedAtAfterOrderByUpdatedAtDesc(storeId, since)
                .flatMap(this::enrichOrder);
    }

    @Override
    public Mono<OrderDTO> unassignOrderToCourier(Long id) {
        return orderRepository.findById(id).flatMap(existingOrder -> {
            if (existingOrder.getStatus() == OrderStatus.ASSIGNED) {
                existingOrder.setCourierId(null);
                existingOrder.setUpdatedAt(LocalDateTime.now());
            } else {
                return Mono.error(new Throwable());
            }
            return orderRepository.save(existingOrder);
        }).map(OrderMapper::toDTO);
    }


    private Mono<OrderDisplayDTO> enrichOrder(Order order) {
        // Récupération des informations du magasin
        Mono<StoreDTO> storeMono = storeService.getStoreById(order.getStoreId());

        // Pour le courier, si order.getCourierId() est null ou que le service ne trouve rien,
        // on fournit un objet CourierDTO par défaut.
        Mono<CourierDTO> courierMono;
        if (order.getCourierId() != null) {
            courierMono = courierService.getCourierById(order.getCourierId())
                    .defaultIfEmpty(new CourierDTO());
        } else {
            // Si l'id du courier est null, on crée directement un CourierDTO par défaut.
            courierMono = Mono.just(new CourierDTO());
        }

        // Combine les deux appels asynchrones et enrichit l'objet Order en OrderDisplayDTO
        return Mono.zip(storeMono, courierMono)
                .map(tuple -> {
                    StoreDTO storeDTO = tuple.getT1();
                    CourierDTO courierDTO = tuple.getT2();

                    // Extraction des informations enrichissantes
                    String storeName = storeDTO.getName();
                    String storeAddress = storeDTO.getAddress();
                    String courierName = courierDTO.getName();

                    // Conversion de Order en OrderDisplayDTO enrichi
                    return OrderMapper.toDisplayDTO(order, storeName, storeAddress, courierName);
                });
    }

}
