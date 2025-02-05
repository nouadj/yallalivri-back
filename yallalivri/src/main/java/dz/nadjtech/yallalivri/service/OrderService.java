package dz.nadjtech.yallalivri.service;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.dto.OrderStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {
    Flux<OrderDTO> getAllOrders();
    Mono<OrderDTO> getOrderById(Long id);
    Mono<OrderDTO> createOrder(OrderDTO orderDTO);
    Mono<OrderDTO> updateOrder(Long id, OrderDTO orderDTO);
    Mono<OrderDTO> updateOrderStatus(Long id, OrderStatus newOrderStatus);
    Mono<Void> deleteOrder(Long id);
    Flux<OrderDTO> getAllOrderByCourierId(Long courierId);
    Flux<OrderDTO> getAllOrderByStoreId(Long storeId);
}
