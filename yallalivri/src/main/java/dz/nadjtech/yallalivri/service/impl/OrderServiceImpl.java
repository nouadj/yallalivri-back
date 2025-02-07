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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

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
        if (order.getStoreLatitude() == null || order.getStoreLongitude() == null) {
            return; // Évite une erreur si la position n'est pas renseignée
        }

        Point storeLocation = new Point(order.getStoreLatitude(), order.getStoreLongitude());
        double maxDistanceKm = 20.0; // Rayon de recherche (20 km)

        userRepository.findAllByRole(UserRole.COURIER)
                .filter(user -> user.getNotificationToken() != null && user.getLatitude() != null && user.getLongitude() != null)
                .filter(courier -> {
                    Point courierLocation = new Point(courier.getLatitude(), courier.getLongitude());
                    double distance = calculateDistance(storeLocation, courierLocation);
                    return distance <= maxDistanceKm; // Filtre les livreurs proches
                })
                .doOnNext(courier -> sendPushNotification(courier.getNotificationToken(), order))
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

    private void sendPushNotification(String expoToken, OrderDTO order) {
        WebClient.create("https://exp.host")
                .post()
                .uri("/--/api/v2/push/send")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(Map.of(
                        "to", expoToken,
                        "title", "📦 Nouvelle commande disponible !",
                        "body", "Une nouvelle commande de " + order.getCustomerName() + " est disponible.",
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
                   if( isValidTransition(existingOrder.getStatus(), newOrderStatus) ) {
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
            case ASSIGNED  ->
                // From SHIPPED, only allow RETURNED or DELIVERED
                    newStatus == OrderStatus.RETURNED || newStatus == OrderStatus.DELIVERED;
            default -> false;
        };
    }

    @Override
    public Mono<Void> deleteOrder(Long id) {
        return orderRepository.deleteById(id);
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
    public Flux<OrderDisplayDTO> getOrdersByStatusSince(String status, LocalDateTime since) {
        return orderRepository.findByStatusAndCreatedAtAfterOrderByUpdatedAtDesc(status, since).flatMap(this::enrichOrder);
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
        return  orderRepository.findById(id) .flatMap(existingOrder -> {
            if ( existingOrder.getStatus() == OrderStatus.ASSIGNED ) {
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
