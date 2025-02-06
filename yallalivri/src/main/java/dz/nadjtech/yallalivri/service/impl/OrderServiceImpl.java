package dz.nadjtech.yallalivri.service.impl;

import dz.nadjtech.yallalivri.dto.OrderDTO;
import dz.nadjtech.yallalivri.entity.Order;
import dz.nadjtech.yallalivri.mapper.OrderMapper;
import dz.nadjtech.yallalivri.dto.OrderStatus;
import dz.nadjtech.yallalivri.repository.CourierRepository;
import dz.nadjtech.yallalivri.repository.OrderRepository;
import dz.nadjtech.yallalivri.service.OrderService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CourierRepository courierRepository;

    public OrderServiceImpl(OrderRepository orderRepository, CourierRepository courierRepository) {
        this.orderRepository = orderRepository;
        this.courierRepository = courierRepository;
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
        Order orderEntity = OrderMapper.toEntity(orderDTO);
        return orderRepository.save(orderEntity)
                .map(OrderMapper::toDTO);
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
                    existingOrder.setTotalAmount(orderDTO.getTotalAmount());
                    existingOrder.setUpdatedAt(orderDTO.getUpdatedAt());
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
    public Flux<OrderDTO> getAllOrderByCourierId(Long courierId) {
        return orderRepository.findByCourierIdOrderByUpdatedAtDesc(courierId)
                .map(OrderMapper::toDTO);
    }

    @Override
    public Flux<OrderDTO> getAllOrderByStoreId(Long storeId) {
        return orderRepository.findByStoreIdOrderByUpdatedAtDesc(storeId)
                .map(OrderMapper::toDTO);
    }

    @Override
    public Flux<OrderDTO> getOrdersByStatusSince(String status, LocalDateTime since) {
        return orderRepository.findByStatusAndCreatedAtAfter(status, since).map(OrderMapper::toDTO);
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
    public Flux<OrderDTO> getOrdersByCourierAndStatus(Long courierId, OrderStatus orderStatus) {
        return this.orderRepository.findByCourierIdAndStatus(courierId, orderStatus).map(OrderMapper::toDTO);
    }

    @Override
    public Flux<OrderDTO> getRecentOrdersByStoreId(Long storeId, LocalDateTime since) {
        return orderRepository.findByStoreIdAndCreatedAtAfter(storeId, since)
                .map(OrderMapper::toDTO);
    }

}
