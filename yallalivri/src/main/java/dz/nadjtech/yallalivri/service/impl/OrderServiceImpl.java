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
                // From SHIPPING, only allow SHIPPED or CANCELLED
                    newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED ->
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
        return this.orderRepository.findAll().filter(order -> order.getCourierId().equals(courierId)).map(OrderMapper::toDTO);
    }

    @Override
    public Flux<OrderDTO> getAllOrderByStoreId(Long storeId) {
        return this.orderRepository.findAll().filter(order -> order.getStoreId().equals(storeId)).map(OrderMapper::toDTO);
    }
}
