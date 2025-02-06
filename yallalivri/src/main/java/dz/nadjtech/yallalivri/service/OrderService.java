package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.OrderDTO;
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
    Flux<OrderDTO> getAllOrderByCourierId(Long courierId);
    Flux<OrderDTO> getAllOrderByStoreId(Long storeId);

    Flux<OrderDTO> getOrdersByStatusSince(String status, LocalDateTime since);

    Mono<OrderDTO> assignOrderToCourier(Long id, Map<String, Object> updates);

    Flux<OrderDTO> getOrdersByCourierAndStatus(Long courierId, OrderStatus orderStatus);

    Flux<OrderDTO> getRecentOrdersByStoreId(Long storeId, LocalDateTime since);
}
