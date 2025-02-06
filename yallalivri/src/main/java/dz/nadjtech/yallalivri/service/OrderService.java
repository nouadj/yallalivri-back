package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderDisplayDTO;
import dz.nadjtech.yallalivri.dto.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

public interface OrderService {
    Flux<OrderDTO> getAllOrders();
    Mono<OrderDTO> getOrderById(Long id);
    Mono<OrderDTO> createOrder(OrderDTO orderDTO);
    Mono<OrderDTO> updateOrder(Long id, OrderDTO orderDTO);
    Mono<OrderDTO> updateOrderStatus(Long id, OrderStatus newOrderStatus);
    Mono<Void> deleteOrder(Long id);
    Flux<OrderDisplayDTO> getAllOrderByCourierId(Long courierId);
    Flux<OrderDisplayDTO> getAllOrderByStoreId(Long storeId);

    Flux<OrderDisplayDTO> getOrdersByStatusSince(String status, LocalDateTime since);

    Mono<OrderDTO> assignOrderToCourier(Long id, Map<String, Object> updates);

    Flux<OrderDisplayDTO> getOrdersByCourierAndStatus(Long courierId, OrderStatus orderStatus);

    Flux<OrderDisplayDTO> getRecentOrdersByStoreId(Long storeId, LocalDateTime since);
}
